package org.fl.flowledger.auth.service;

import org.fl.flowledger.auth.dto.LoginDto;
import org.fl.flowledger.auth.dto.LoginResult;
import org.fl.flowledger.auth.dto.RefreshResult;
import org.fl.flowledger.user.dto.CreateUserDto;

import java.net.InetAddress;

public interface AuthService {
    String register(CreateUserDto dto);
    LoginResult login(LoginDto dto);
    String logout(String refreshToken, InetAddress ipAddress);
    RefreshResult refresh(String refreshToken);
    Long getCurrentUserId();
}
