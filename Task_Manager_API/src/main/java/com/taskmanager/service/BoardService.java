package com.taskmanager.service;

import com.taskmanager.domain.Board;
import com.taskmanager.domain.BoardAccessMode;
import com.taskmanager.domain.BoardTeam;
import com.taskmanager.domain.Organization;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.Team;
import com.taskmanager.domain.User;
import com.taskmanager.repository.BoardRepository;
import com.taskmanager.repository.BoardTeamRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.BoardRequest;
import com.taskmanager.web.api.dto.BoardResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

    private static final Logger log = LoggerFactory.getLogger(BoardService.class);

    private final BoardRepository boardRepository;
    private final BoardTeamRepository boardTeamRepository;
    private final ProjectService projectService;
    private final ProjectAccessService projectAccessService;
    private final BoardAccessService boardAccessService;
    private final TeamService teamService;
    private final CurrentUserService currentUserService;
    private final BoardEventPublisher boardEventPublisher;

    public BoardService(
            BoardRepository boardRepository,
            BoardTeamRepository boardTeamRepository,
            ProjectService projectService,
            ProjectAccessService projectAccessService,
            BoardAccessService boardAccessService,
            TeamService teamService,
            CurrentUserService currentUserService,
            BoardEventPublisher boardEventPublisher) {
        this.boardRepository = boardRepository;
        this.boardTeamRepository = boardTeamRepository;
        this.projectService = projectService;
        this.projectAccessService = projectAccessService;
        this.boardAccessService = boardAccessService;
        this.teamService = teamService;
        this.currentUserService = currentUserService;
        this.boardEventPublisher = boardEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> listBoards(Long projectId) {
        Project project = projectService.getProjectOrThrow(projectId);
        User user = currentUserService.getCurrentUser();
        projectAccessService.requireCanRead(project, user);
        return boardRepository.findByProjectOrderByCreatedAtAsc(project).stream()
                .filter(board -> boardAccessService.canAccessBoard(board, user))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BoardResponse getBoard(Long boardId) {
        Board board = getBoardOrThrow(boardId);
        boardAccessService.requireCanViewBoard(board, currentUserService.getCurrentUser());
        return toResponse(board);
    }

    @Transactional
    public BoardResponse createBoard(Long projectId, BoardRequest request) {
        Project project = projectService.getProjectOrThrow(projectId);
        User user = currentUserService.getCurrentUser();
        projectAccessService.requireCanWriteContent(project, user);

        BoardAccessMode mode = request.accessModeOrDefault();
        boardAccessService.assertCanSetAccessMode(project, user, mode);

        Board board = new Board();
        board.setProject(project);
        board.setName(request.name());
        board.setAccessMode(mode);
        board.setCreatedBy(user);
        board = boardRepository.save(board);

        if (mode == BoardAccessMode.TEAM_ACL) {
            replaceBoardTeams(board, request.teamIds());
        }

        log.info("Board created: id={}, projectId={}, accessMode={}", board.getId(), projectId, mode);
        return toResponse(board);
    }

    @Transactional
    public BoardResponse updateBoard(Long boardId, BoardRequest request) {
        Board board = getBoardOrThrow(boardId);
        User user = currentUserService.getCurrentUser();
        boardAccessService.requireCanWriteBoard(board, user);

        board.setName(request.name());

        if (request.accessMode() != null) {
            boardAccessService.assertCanSetAccessMode(board.getProject(), user, request.accessMode());
            board.setAccessMode(request.accessMode());
        }

        BoardAccessMode mode = board.getAccessMode() == null ? BoardAccessMode.OPEN : board.getAccessMode();
        if (mode == BoardAccessMode.TEAM_ACL) {
            if (request.teamIds() != null || request.accessMode() == BoardAccessMode.TEAM_ACL) {
                if (!boardAccessService.canSetTeamAcl(board.getProject(), user)) {
                    throw new ApiException(HttpStatus.FORBIDDEN.value(),
                            "Only project owner or organization admin can manage TEAM_ACL teams");
                }
                replaceBoardTeams(board, request.teamIds() == null ? List.of() : request.teamIds());
            }
        } else if (request.accessMode() != null) {
            boardTeamRepository.deleteByBoard(board);
        }

        board = boardRepository.save(board);
        boardEventPublisher.publishAfterCommit("BOARD_UPDATED", board.getId(), null);
        return toResponse(board);
    }

    @Transactional
    public void deleteBoard(Long boardId) {
        Board board = getBoardOrThrow(boardId);
        boardAccessService.requireCanWriteBoard(board, currentUserService.getCurrentUser());
        boardTeamRepository.deleteByBoard(board);
        boardRepository.delete(board);
        log.info("Board deleted: id={}", boardId);
    }

    public Board getBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Board not found"));
    }

    private void replaceBoardTeams(Board board, List<Long> teamIds) {
        boardTeamRepository.deleteByBoard(board);
        if (teamIds == null || teamIds.isEmpty()) {
            return;
        }
        Organization organization = board.getProject().getOrganization();
        if (organization == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                    "TEAM_ACL boards require a project linked to an organization");
        }
        for (Long teamId : teamIds) {
            Team team = teamService.getTeamInOrgOrThrow(organization.getId(), teamId);
            BoardTeam link = new BoardTeam();
            link.setBoard(board);
            link.setTeam(team);
            boardTeamRepository.save(link);
        }
    }

    private BoardResponse toResponse(Board board) {
        List<Long> teamIds = boardTeamRepository.findByBoard(board).stream()
                .map(bt -> bt.getTeam().getId())
                .toList();
        return BoardResponse.from(board, new ArrayList<>(teamIds));
    }
}
