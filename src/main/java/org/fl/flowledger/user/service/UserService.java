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
    ResponseEntity<UserResponse> getUser(UUID uuid);

    ResponseEntity<UserResponse> getUserByEmail(String email);

    ResponseEntity<String> updateUser(UpdateUserRequest user);
    ResponseEntity<String> changePassword(ChangePasswordDto changePasswordDto);

    ResponseEntity<String> deleteUser(UUID uuid);
}
