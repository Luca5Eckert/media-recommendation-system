package com.mrs.user_service.service;

import com.mrs.user_service.dto.LoginUserRequest;
import com.mrs.user_service.dto.RegisterUserRequest;
import com.mrs.user_service.handler.auth.LoginUserHandler;
import com.mrs.user_service.handler.auth.RegisterUserHandler;
import com.mrs.user_service.model.RoleUser;
import com.mrs.user_service.model.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final LoginUserHandler loginUserHandler;
    private final RegisterUserHandler registerUserHandler;

    public AuthService(LoginUserHandler loginUserHandler, RegisterUserHandler registerUserHandler) {
        this.loginUserHandler = loginUserHandler;
        this.registerUserHandler = registerUserHandler;
    }

    public void register(RegisterUserRequest registerUserRequest) {
        UserEntity user = UserEntity.builder()
                .name(registerUserRequest.name())
                .email(registerUserRequest.email())
                .password(registerUserRequest.password())
                .role(RoleUser.USER)
                .build();

        registerUserHandler.execute(user);
    }

    public String login(LoginUserRequest loginUserRequest){
        return loginUserHandler.execute(
                loginUserRequest.email(),
                loginUserRequest.password()
        );
    }


}
