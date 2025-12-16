package com.mrs.user_service.handler;

import com.mrs.user_service.model.UserEntity;
import com.mrs.user_service.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class GetUserHandler {

    private final UserRepository userRepository;

    public GetUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserEntity execute(UUID userId){
        return userRepository.findById(userId)
                .orElseThrow( () -> new IllegalArgumentException("User not found by id"));
    }

}
