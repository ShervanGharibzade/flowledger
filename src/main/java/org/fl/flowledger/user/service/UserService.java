package org.fl.flowledger.user.service;

import lombok.RequiredArgsConstructor;
import org.fl.flowledger.user.dto.ChangePasswordDto;
import org.fl.flowledger.user.dto.UpdateUserRequest;
import org.fl.flowledger.user.dto.UserResponse;
import org.fl.flowledger.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface UserService {
    UserResponse getUser(UUID uuid);

    UserResponse getUserByEmail(String email);

    String updateUser(UpdateUserRequest user);
    String changePassword(ChangePasswordDto changePasswordDto);

    String deleteUser(UUID uuid);
}
