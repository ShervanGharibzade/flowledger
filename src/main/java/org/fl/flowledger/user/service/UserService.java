package org.fl.flowledger.user.service;

import org.fl.flowledger.user.dto.ChangePasswordDto;
import org.fl.flowledger.user.dto.UpdateUserRequest;
import org.fl.flowledger.user.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface UserService {
    UserResponse getUser(UUID uuid);

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);

    String updateUser(UpdateUserRequest user);

    String changePassword(Long userId, ChangePasswordDto changePasswordDto);

    String deleteUser(UUID uuid);
}
