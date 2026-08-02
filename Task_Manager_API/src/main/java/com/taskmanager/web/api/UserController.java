package com.taskmanager.web.api;

import com.taskmanager.config.ApiConstants;
import com.taskmanager.domain.User;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.service.InvitationService;
import com.taskmanager.service.NotificationService;
import com.taskmanager.service.UserService;
import com.taskmanager.web.api.dto.InvitationResponse;
import com.taskmanager.web.api.dto.NotificationResponse;
import com.taskmanager.web.api.dto.UpdateProfileRequest;
import com.taskmanager.web.api.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = ApiConstants.API_V1, produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final UserService userService;
    private final NotificationService notificationService;
    private final InvitationService invitationService;
    private final CurrentUserService currentUserService;

    public UserController(
            UserService userService,
            NotificationService notificationService,
            InvitationService invitationService,
            CurrentUserService currentUserService) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.invitationService = invitationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/users/me")
    public UserResponse getProfile() {
        return userService.getCurrentUserProfile();
    }

    @PutMapping("/users/me")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateCurrentUserProfile(request);
    }

    @GetMapping("/users/me/notifications")
    public List<NotificationResponse> listNotifications() {
        User user = currentUserService.getCurrentUser();
        return notificationService.listForUser(user).stream().map(NotificationResponse::from).toList();
    }

    @PutMapping("/users/me/notifications/{notificationId}/read")
    public NotificationResponse markNotificationRead(@PathVariable Long notificationId) {
        return NotificationResponse.from(
                notificationService.markRead(notificationId, currentUserService.getCurrentUser()));
    }

    @GetMapping("/users/me/invitations")
    public List<InvitationResponse> myInvitations() {
        return invitationService.listMine();
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public InvitationResponse acceptInvitation(@PathVariable Long invitationId) {
        return invitationService.accept(invitationId);
    }

    @PostMapping("/invitations/{invitationId}/decline")
    public InvitationResponse declineInvitation(@PathVariable Long invitationId) {
        return invitationService.decline(invitationId);
    }
}
