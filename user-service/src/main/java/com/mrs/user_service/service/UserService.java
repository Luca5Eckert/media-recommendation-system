package com.mrs.user_service.service;

import com.mrs.user_service.dto.CreateUserRequest;
import com.mrs.user_service.handler.CreateUserHandler;
import com.mrs.user_service.model.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final CreateUserHandler createUserHandler;

    public UserService(CreateUserHandler createUserHandler) {
        this.createUserHandler = createUserHandler;
    }

    public void create(CreateUserRequest createUserRequest) {
        UserEntity user = UserEntity.builder()
                .name(createUserRequest.name())
                .email(createUserRequest.email())
                .password(createUserRequest.password())
                .fullName(createUserRequest.fullname())
                .build();

        createUserHandler.handler(user);
    }

}
