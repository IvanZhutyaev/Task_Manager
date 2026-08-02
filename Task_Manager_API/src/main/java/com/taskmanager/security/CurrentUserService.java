package com.taskmanager.security;

import com.taskmanager.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    private final CustomUserDetailsService userDetailsService;

    public CurrentUserService(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }
        String email = authentication.getName();
        return userDetailsService.loadUserEntityByEmail(email);
    }
}
