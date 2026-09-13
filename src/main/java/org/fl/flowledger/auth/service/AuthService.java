package org.fl.flowledger.auth.service;

import org.fl.flowledger.auth.dto.LoginDto;
import org.fl.flowledger.auth.dto.LoginResponse;
import org.fl.flowledger.auth.dto.LoginResult;
import org.fl.flowledger.auth.dto.TokenResponse;
import org.fl.flowledger.user.dto.ChangePasswordDto;
import org.fl.flowledger.user.dto.CreateUserDto;
import org.fl.flowledger.user.dto.UserResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    String register(CreateUserDto dto) throws IllegalAccessException;
    LoginResult login(LoginDto dto);
    String logout();
    String changePassword(ChangePasswordDto dto);
    TokenResponse refresh(String refreshToken);
}
