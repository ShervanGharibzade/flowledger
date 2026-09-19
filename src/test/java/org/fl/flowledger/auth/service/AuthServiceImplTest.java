package org.fl.flowledger.auth.service;

import org.fl.flowledger.audit.service.AuditService;
import org.fl.flowledger.auth.AuthMapper.AuthMapper;
import org.fl.flowledger.auth.dto.LoginDto;
import org.fl.flowledger.auth.dto.LoginResult;
import org.fl.flowledger.auth.dto.RefreshResult;
import org.fl.flowledger.common.exception.EmailAlreadyUsedException;
import org.fl.flowledger.common.exception.ResourceNotFoundException;
import org.fl.flowledger.common.exception.UnauthorizedException;
import org.fl.flowledger.common.exception.UserNotFoundedException;
import org.fl.flowledger.common.security.JwtService;
import org.fl.flowledger.common.security.RefreshTokenService;
import org.fl.flowledger.user.dto.CreateUserDto;
import org.fl.flowledger.user.dto.UserRoles;
import org.fl.flowledger.user.dto.UserStatus;
import org.fl.flowledger.user.entity.User;
import org.fl.flowledger.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthMapper authMapper;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User buildUser(Long id, UserRoles role) {
        return User.builder()
                .id(id)
                .uuid(UUID.randomUUID())
                .email("jane@example.com")
                .passwordHash("hashed")
                .firstName("Jane")
                .lastName("Doe")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    @Test
    void register_throwsWhenEmailAlreadyUsed() {
        CreateUserDto dto = new CreateUserDto("jane@example.com", "password123", "Jane", "Doe");
        when(userRepository.findByEmail(dto.email()))
                .thenReturn(Optional.of(buildUser(1L, UserRoles.USER)));

        assertThrows(EmailAlreadyUsedException.class, () -> authService.register(dto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_hashesPasswordAndSavesActiveUser_whenEmailAvailable() {
        CreateUserDto dto = new CreateUserDto("new@example.com", "password123", "New", "User");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        String result = authService.register(dto);

        assertNotNull(result);
        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("new@example.com")
                        && u.getPasswordHash().equals("hashed-password")
                        && u.getRole() == UserRoles.USER
                        && u.getStatus() == UserStatus.ACTIVE
        ));
    }


    @Test
    void login_issuesAccessTokenWithBareRoleName_notAuthorityPrefixed() {
        LoginDto dto = new LoginDto("jane@example.com", "password123");
        User user = buildUser(42L, UserRoles.ADMIN);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("42", "ADMIN")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(42L)).thenReturn("refresh-token");
        when(authMapper.toData(any(LoginResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoginResult result = authService.login(dto);

        verify(jwtService).generateAccessToken(eq("42"), eq("ADMIN"));
        verify(jwtService, never()).generateAccessToken(any(), eq("ROLE_ADMIN"));

        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals(user.getEmail(), result.email());

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password())
        );
    }

    @Test
    void login_throwsResourceNotFound_whenAuthenticatedButUserRecordMissing() {
        LoginDto dto = new LoginDto("ghost@example.com", "password123");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(dto));
    }


    @Test
    void refresh_rotatesRefreshTokenAndIssuesNewAccessTokenWithBareRole() {
        User user = buildUser(7L, UserRoles.USER);

        when(refreshTokenService.getUserId("old-refresh")).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("7", "USER")).thenReturn("new-access-token");
        when(refreshTokenService.rotate("old-refresh", 7L)).thenReturn("new-refresh-token");

        RefreshResult result = authService.refresh("old-refresh");

        assertEquals("new-access-token", result.accessToken());
        assertEquals("new-refresh-token", result.refreshToken());

        verify(refreshTokenService).rotate("old-refresh", 7L);
    }

    @Test
    void refresh_throwsUserNotFounded_whenUserNoLongerExists() {
        when(refreshTokenService.getUserId("stale-refresh")).thenReturn(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundedException.class, () -> authService.refresh("stale-refresh"));

        verify(refreshTokenService, never()).rotate(any(), any());
    }


    @Test
    void logout_revokesRefreshTokenAndRecordsAudit() throws Exception {
        User user = buildUser(3L, UserRoles.USER);
        InetAddress ip = InetAddress.getByName("127.0.0.1");

        when(refreshTokenService.getUserId("refresh-token")).thenReturn(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        String result = authService.logout("refresh-token", ip);

        assertNotNull(result);
        verify(refreshTokenService).revoke("refresh-token");
        verify(auditService).log(
                eq(user),
                any(),
                any(),
                eq(user.getUuid()),
                any(),
                eq(ip)
        );
    }


    @Test
    void getCurrentUserId_parsesNumericPrincipalNameDirectly() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("123", null, "ROLE_USER")
        );

        Long userId = authService.getCurrentUserId();

        assertEquals(123L, userId);
        verifyNoInteractions(userRepository);
    }

    @Test
    void getCurrentUserId_throwsUnauthorized_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThrows(UnauthorizedException.class, () -> authService.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_throwsUnauthorized_whenPrincipalNameIsNotNumeric() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("jane@example.com", null, "ROLE_USER")
        );

        assertThrows(UnauthorizedException.class, () -> authService.getCurrentUserId());
    }
}
