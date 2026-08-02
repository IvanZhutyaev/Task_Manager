package com.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.taskmanager.domain.User;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.CustomUserDetailsService;
import com.taskmanager.security.JwtService;
import com.taskmanager.web.api.dto.AuthResponse;
import com.taskmanager.web.api.dto.LoginRequest;
import com.taskmanager.web.api.dto.RegisterRequest;
import com.taskmanager.web.exception.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("user@test.com", "secret123", "User");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(userDetailsService.loadUserByUsername(request.email())).thenReturn(
                org.springframework.security.core.userdetails.User.withUsername(request.email())
                        .password("hash")
                        .roles("USER")
                        .build());
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token-123");

        AuthResponse response = authService.register(request);

        assertEquals("token-123", response.token());
        assertEquals("user@test.com", response.user().email());
    }

    @Test
    void registerFailsWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("user@test.com", "secret123", "User");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));
        assertEquals(409, ex.getStatus());
    }

    @Test
    void loginReturnsToken() {
        LoginRequest request = new LoginRequest("user@test.com", "secret123");
        User user = new User();
        user.setId(1L);
        user.setEmail(request.email());
        user.setName("User");

        when(userDetailsService.loadUserEntityByEmail(request.email())).thenReturn(user);
        when(userDetailsService.loadUserByUsername(request.email())).thenReturn(
                org.springframework.security.core.userdetails.User.withUsername(request.email())
                        .password("hash")
                        .roles("USER")
                        .build());
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token-456");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        AuthResponse response = authService.login(request);

        assertNotNull(response.token());
        assertEquals("User", response.user().name());
    }
}
