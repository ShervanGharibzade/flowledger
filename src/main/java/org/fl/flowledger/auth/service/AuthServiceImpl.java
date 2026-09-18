package org.fl.flowledger.auth.service;

import lombok.RequiredArgsConstructor;
import org.fl.flowledger.audit.dto.AuditAction;
import org.fl.flowledger.audit.dto.AuditEntityType;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    @Override
    public String register(CreateUserDto dto) {
        Optional<User> user = userRepository.findByEmail(dto.email());

        if (user.isPresent()) {
            throw new EmailAlreadyUsedException();
        }

        String hashPassword = passwordEncoder.encode(dto.password());

        User newUser = User.builder()
                .email(dto.email())
                .role(UserRoles.USER)
                .firstName(dto.firstName())
                .status(UserStatus.ACTIVE)
                .lastName(dto.LastName())
                .passwordHash(hashPassword)
                .build();

        userRepository.save(newUser);

        return "User registered successfully now you can login.";
    }

    @Override
    public LoginResult login(LoginDto dto) {

        // authenticationManager verifies the password; on failure it throws
        // org.springframework.security.authentication.BadCredentialsException,
        // which GlobalExceptionHandler maps to a generic 401.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.email(),
                        dto.password()
                )
        );

        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "email",
                                dto.email()
                        )
                );

        // IMPORTANT: store the bare role name here (e.g. "USER"), never a
        // "ROLE_"-prefixed authority string. JwtAuthenticationConverter is the
        // single place that adds the "ROLE_" prefix when the token is read back;
        // if this claim already carried the prefix, converted authorities would
        // come out as "ROLE_ROLE_USER" and every hasRole()/hasAnyRole() check
        // would silently fail for freshly-issued login tokens.
        String token = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getRole().name()
        );

        String refreshToken =
                refreshTokenService.createRefreshToken(user.getId());

        LoginResult data = new LoginResult(
                user.getEmail(),
                user.getUuid(),
                token,
                refreshToken
        );

        return authMapper.toData(data);
    }


    @Override
    public RefreshResult refresh(String refreshToken) {

        Long userId =
                refreshTokenService.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundedException::new);

        String accessToken =
                jwtService.generateAccessToken(
                        user.getId().toString(),
                        user.getRole().name()
                );

        // Rotate the refresh token on every use: the old one is revoked and a
        // new one issued. If a refresh token is ever reused after rotation
        // (e.g. it leaked and was already consumed by an attacker or by the
        // legitimate client), the old key will no longer resolve and
        // refreshTokenService.getUserId() above will reject it.
        String newRefreshToken =
                refreshTokenService.rotate(refreshToken, userId);

        return new RefreshResult(accessToken, newRefreshToken);
    }

    @Override
    public String logout(String refreshToken, InetAddress ipAddress) {
        Long userId = refreshTokenService.getUserId(refreshToken);

        User user = userRepository.findById(userId).orElseThrow(
                ()-> new ResourceNotFoundException("user",userId)
        );

        refreshTokenService.revoke(refreshToken);

        auditService.log(
                user,
                AuditAction.LOGOUT,
                AuditEntityType.USER,
                user.getUuid(),
                Map.of(),
                ipAddress
        );

        return "User logout successfully";
    }

    @Override
    public Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException();
        }

        // JwtService puts the user's numeric id (not the email) in the JWT
        // "sub" claim, and JwtAuthenticationConverter carries that subject
        // straight through as authentication.getName(). So the principal
        // name here IS the user id already - parse it directly rather than
        // treating it as an email and looking it up (which would never
        // match and would make every call here fail).
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new UnauthorizedException();
        }
    }
}
