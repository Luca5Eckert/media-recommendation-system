package com.mrs.user_service.handler.user;

import com.mrs.user_service.model.UserEntity;
import com.mrs.user_service.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

@Component
public class CreateUserHandler {

    private final UserRepository userRepository;

    public CreateUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void execute(UserEntity user){
        if(user == null) throw new IllegalArgumentException("User can't be null");

        if(userRepository.existsByEmail(user.getEmail())){
            throw new IllegalArgumentException("User's email already in use");
        }

        userRepository.save(user);
    }

}
