package org.fl.flowledger.user.service;


import lombok.RequiredArgsConstructor;
import org.fl.flowledger.common.exception.BadCredentialsException;
import org.fl.flowledger.common.exception.ResourceNotFoundException;
import org.fl.flowledger.common.exception.UserNotFoundedException;
import org.fl.flowledger.common.security.RefreshTokenService;
import org.fl.flowledger.user.dto.ChangePasswordDto;
import org.fl.flowledger.user.dto.UpdateUserRequest;
import org.fl.flowledger.user.dto.UserResponse;
import org.fl.flowledger.user.entity.User;
import org.fl.flowledger.user.mapper.UserMapper;
import org.fl.flowledger.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(UUID uuid) {

        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                uuid
                        )
                );

        return userMapper.toResponse(user);

    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundedException::new);

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "email",
                                email
                        )
                );

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public String updateUser(UpdateUserRequest req) {

        User user = userRepository.findByUuid(req.uuid())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                req.uuid()
                        )
                );

        userMapper.updateUserFromRequest(req, user);

        return "User with this uuid %s is updated".formatted(req.uuid());
    }

    @Override
    @Transactional
    public String changePassword(Long userId, ChangePasswordDto dto) {

        // userId comes from the authenticated principal (see UserController),
        // never from client input, so this can only ever change the caller's
        // own password.
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundedException::new);

        if (!passwordEncoder.matches(
                dto.currentPassword(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException();
        }

        if (passwordEncoder.matches(
                dto.newPassword(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "New password must be different from the current password"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(dto.newPassword())
        );

        // Force re-authentication on every other device/session once the
        // password changes, so a stolen refresh token stops working too.
        refreshTokenService.revokeAll(userId);

        return "Password changed successfully";
    }

    @Override
    @Transactional
    public String deleteUser(UUID uuid) {

        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                uuid
                        )
                );

        userRepository.delete(user);

        return "User deleted successfully.";
    }
}
