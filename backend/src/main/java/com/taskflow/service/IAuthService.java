package com.taskflow.service;

import com.taskflow.dto.AuthDto;

public interface IAuthService {
    AuthDto.TokenResponse register(AuthDto.RegisterRequest req);
    AuthDto.TokenResponse login(AuthDto.LoginRequest req);
}
