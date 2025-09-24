package com.useronboard.service.service;

import com.useronboard.service.dto.UserRegistrationRequest;
import com.useronboard.service.dto.UserResponse;
import com.useronboard.service.entity.User;
import com.useronboard.service.entity.UserStatus;
import com.useronboard.service.messaging.UserEventPublisher;
import com.useronboard.service.repository.UserAuditLogRepository;
import com.useronboard.service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest validRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validRequest = new UserRegistrationRequest();
        validRequest.setEmail("test@example.com");
        validRequest.setPassword("password123");
        validRequest.setFirstName("John");
        validRequest.setLastName("Doe");

        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashed-password");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setStatus(UserStatus.PENDING);
    }

    @Test
    void registerUser_Success() {
        // Arrange
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse result = userService.registerUser(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals(UserStatus.PENDING, result.getStatus());

        verify(userRepository).existsByEmailIgnoreCase("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(auditLogRepository).save(any());
        verify(eventPublisher).publishUserRegisteredEvent(any(User.class));
    }

    @Test
    void registerUser_EmailAlreadyExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(validRequest)
        );

        assertEquals("User with email test@example.com already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticateUser_Success() {
        // Arrange
        testUser.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        // Act
        Optional<User> result = userService.authenticateUser("test@example.com", "password123");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
    }

    @Test
    void authenticateUser_InvalidPassword_ReturnsEmpty() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "hashed-password")).thenReturn(false);

        // Act
        Optional<User> result = userService.authenticateUser("test@example.com", "wrongpassword");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void authenticateUser_UserNotActive_ThrowsException() {
        // Arrange
        testUser.setStatus(UserStatus.PENDING);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> userService.authenticateUser("test@example.com", "password123")
        );

        assertTrue(exception.getMessage().contains("User account is not active"));
    }

    @Test
    void approveUser_Success() {
        // Arrange
        String adminId = "admin-123";
        String reason = "Approved by admin";

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse result = userService.approveUser("user-123", adminId, reason);

        // Assert
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
        verify(auditLogRepository).save(any());
        verify(eventPublisher).publishUserApprovedEvent(any(User.class), eq(adminId));
    }

    @Test
    void approveUser_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.approveUser("user-123", "admin-123", "reason")
        );

        assertEquals("User not found: user-123", exception.getMessage());
    }

    @Test
    void approveUser_UserNotPending_ThrowsException() {
        // Arrange
        testUser.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> userService.approveUser("user-123", "admin-123", "reason")
        );

        assertTrue(exception.getMessage().contains("User is not in PENDING status"));
    }
}
