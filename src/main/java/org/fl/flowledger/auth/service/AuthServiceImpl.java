package org.fl.flowledger.auth.service;

import lombok.RequiredArgsConstructor;
import org.fl.flowledger.audit.dto.AuditAction;
import org.fl.flowledger.audit.dto.AuditEntityType;
import org.fl.flowledger.audit.service.AuditService;
import org.fl.flowledger.auth.AuthMapper.AuthMapper;
import org.fl.flowledger.auth.dto.LoginDto;
import org.fl.flowledger.auth.dto.LoginResult;
import org.fl.flowledger.auth.dto.TokenResponse;
import org.fl.flowledger.common.exception.EmailAlreadyUsedException;
import org.fl.flowledger.common.exception.ResourceNotFoundException;
import org.fl.flowledger.common.exception.UnauthorizedException;
import org.fl.flowledger.common.exception.UserNotFoundedException;
import org.fl.flowledger.common.security.JwtService;
import org.fl.flowledger.common.security.RefreshTokenService;
import org.fl.flowledger.user.dto.ChangePasswordDto;
import org.fl.flowledger.user.dto.CreateUserDto;
import org.fl.flowledger.user.dto.UserRoles;
import org.fl.flowledger.user.entity.User;
import org.fl.flowledger.user.mapper.UserMapper;
import org.fl.flowledger.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    @Override
    public String register(CreateUserDto dto)  {
        Optional<User> user = userRepository.findByEmail(dto.email());

        if (user.isPresent()) {
            throw new EmailAlreadyUsedException();
        }

        String hashPassword = passwordEncoder.encode(dto.password());

        User newUser = User.builder()
                .email(dto.email())
                .role(UserRoles.USER)
                .firstName(dto.firstName())
                .lastName(dto.LastName())
                .passwordHash(hashPassword)
                .build();

        userRepository.save(newUser);

        return "User registered successfully now you can login.";
    }

    @Override
    public LoginResult login(LoginDto dto) {

        User existsEmail = userRepository.findByEmail(dto.email()).orElseThrow(
                () -> new ResourceNotFoundException("User", "email", dto.email()));

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                existsEmail.getId(),
                                dto.password()
                        )
                );

        String email = authentication.getName();


        String role = authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        String token = jwtService.generateAccessToken(
                email,
                role
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "email", email)
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
    public TokenResponse refresh(String refreshToken) {

        Long userId =
                refreshTokenService.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundedException::new);

        String accessToken =
                jwtService.generateAccessToken(
                        user.getId().toString(),
                        "ROLE_" + user.getRole().name()
                );

        return new TokenResponse(accessToken);
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
    public String changePassword(ChangePasswordDto dto) {
        return null;
    }

    @Override
    public Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException();
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new UnauthorizedException();
        }
    }
}
