package com.taskmanager.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskmanager.android.databinding.ActivityProjectDetailBinding
import kotlinx.coroutines.launch

class ProjectDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PROJECT_ID = "project_id"
    }

    private lateinit var binding: ActivityProjectDetailBinding
    private lateinit var api: ApiClient
    private var projectId: Long = -1
    private lateinit var boardsAdapter: BoardAdapter
    private lateinit var membersAdapter: PersonAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectId = intent.getLongExtra(EXTRA_PROJECT_ID, -1)
        if (projectId < 0) {
            finish()
            return
        }

        binding = ActivityProjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val session = SessionStore(this)
        api = ApiClient(baseUrl = session.baseUrl, token = session.token)

        boardsAdapter = BoardAdapter { board ->
            startActivity(
                Intent(this, BoardActivity::class.java)
                    .putExtra(BoardActivity.EXTRA_PROJECT_ID, projectId)
                    .putExtra(BoardActivity.EXTRA_BOARD_ID, board.id)
                    .putExtra(BoardActivity.EXTRA_BOARD_NAME, board.name)
                    .putExtra(BoardActivity.EXTRA_ACCESS_MODE, board.accessMode ?: "OPEN")
            )
        }
        membersAdapter = PersonAdapter()
        binding.boardsList.layoutManager = LinearLayoutManager(this)
        binding.boardsList.adapter = boardsAdapter
        binding.membersList.layoutManager = LinearLayoutManager(this)
        binding.membersList.adapter = membersAdapter

        binding.backBtn.setOnClickListener { finish() }
        binding.inviteBtn.setOnClickListener { invite() }
        binding.newBoardBtn.setOnClickListener { createBoard() }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        lifecycleScope.launch {
            try {
                val projects = api.listProjects()
                val project = projects.find { it.id == projectId }
                binding.projectTitle.text = project?.name ?: "Проект #$projectId"
                binding.projectMeta.text = buildString {
                    append(project?.description?.ifBlank { null } ?: "Без описания")
                    append(" · роль ")
                    append(project?.currentUserRole ?: "—")
                    if (project?.organizationId != null) append(" · org #${project.organizationId}")
                }

                val boards = api.listBoards(projectId)
                boardsAdapter.submit(boards)
                binding.boardsEmpty.visibility = if (boards.isEmpty()) View.VISIBLE else View.GONE

                val members = api.listMembers(projectId)
                membersAdapter.submit(members.map {
                    Triple(it.name, it.email, it.role)
                })
            } catch (e: Exception) {
                toast(e.message)
            }
        }
    }

    private fun invite() {
        askText("Пригласить в проект", "email@example.com") { email ->
            lifecycleScope.launch {
                try {
                    api.inviteMember(projectId, email, "EDITOR")
                    toast("Приглашение отправлено")
                } catch (e: Exception) {
                    toast(e.message)
                }
            }
        }
    }

    private fun createBoard() {
        askText("Новая доска", "Название доски") { name ->
            lifecycleScope.launch {
                try {
                    api.createBoard(projectId, name)
                    toast("Доска создана")
                    reload()
                } catch (e: Exception) {
                    toast(e.message)
                }
            }
        }
    }
}
