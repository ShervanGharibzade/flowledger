package org.fl.flowledger.user.service;


import lombok.RequiredArgsConstructor;
import org.fl.flowledger.common.exception.BadCredentialsException;
import org.fl.flowledger.common.exception.ResourceNotFoundException;
import org.fl.flowledger.user.dto.ChangePasswordDto;
import org.fl.flowledger.user.dto.UpdateUserRequest;
import org.fl.flowledger.user.dto.UserResponse;
import org.fl.flowledger.user.entity.User;
import org.fl.flowledger.user.mapper.UserMapper;
import org.fl.flowledger.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
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

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<UserResponse> getUser(UUID uuid) {

        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                uuid
                        )
                );

        return ResponseEntity.ok(
                userMapper.toResponse(user)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<UserResponse> getUserByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "email",
                                email
                        )
                );

        return ResponseEntity.ok(
                userMapper.toResponse(user)
        );
    }

    @Override
    @Transactional
    public ResponseEntity<String> updateUser(UpdateUserRequest req) {

        User user = userRepository.findByUuid(req.uuid())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                req.uuid()
                        )
                );

        userMapper.updateUserFromRequest(req, user);

        return ResponseEntity.ok(
                "User with this uuid %s is updated"
                        .formatted(req.uuid())
        );
    }

    @Override
    @Transactional
    public ResponseEntity<String> changePassword(ChangePasswordDto dto) {

        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "email",
                                dto.email()
                        )
                );

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

        return ResponseEntity.ok(
                "Password changed successfully"
        );
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteUser(UUID uuid) {

        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                uuid
                        )
                );

        userRepository.delete(user);

        return ResponseEntity.ok(
                "User deleted successfully."
        );
    }
}