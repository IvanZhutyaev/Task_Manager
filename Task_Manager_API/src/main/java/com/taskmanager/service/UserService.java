package com.taskmanager.service;

import com.taskmanager.domain.User;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.UpdateProfileRequest;
import com.taskmanager.web.api.dto.UserResponse;
import com.taskmanager.web.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public UserService(UserRepository userRepository, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public UserResponse getCurrentUserProfile() {
        return UserResponse.from(currentUserService.getCurrentUser());
    }

    @Transactional
    public UserResponse updateCurrentUserProfile(UpdateProfileRequest request) {
        User user = currentUserService.getCurrentUser();
        user.setName(request.name());
        return UserResponse.from(userRepository.save(user));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "User not found"));
    }
}
