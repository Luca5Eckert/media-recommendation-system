package com.mrs.user_service.handler;

import com.mrs.user_service.model.UserEntity;
import com.mrs.user_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class CreateUserHandler {

    private final UserRepository userRepository;

    public CreateUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void handler(UserEntity user){
        if(user == null) throw new IllegalArgumentException("User can't be null");

        userRepository.save(user);
    }

}
