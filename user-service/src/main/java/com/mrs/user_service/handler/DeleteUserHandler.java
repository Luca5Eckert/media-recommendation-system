package com.mrs.user_service.handler;

import com.mrs.user_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeleteUserHandler {

    private final UserRepository userRepository;

    public DeleteUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void handler(UUID userId){
        if(!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not exist");
        }

        userRepository.deleteById(userId);
    }

}
