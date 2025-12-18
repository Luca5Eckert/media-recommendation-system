package com.mrs.user_service.handler.auth;

import com.mrs.user_service.dto.RegisterUserRequest;
import com.mrs.user_service.model.RoleUser;
import com.mrs.user_service.model.UserEntity;
import com.mrs.user_service.repository.UserRepository;
import com.mrs.user_service.validator.password.PasswordValidatorAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;

public class RegisterUserHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidatorAdapter passwordValidatorAdapter;

    public RegisterUserHandler(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordValidatorAdapter passwordValidatorAdapter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidatorAdapter = passwordValidatorAdapter;
    }


    public void register(UserEntity userEntity){
        if(userEntity == null) throw new IllegalArgumentException("User can't be null");

        if(userRepository.existsByEmail(userEntity.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        String passwordEncoded = passwordEncoder.encode(userEntity.getPassword());
        userEntity.setPassword(passwordEncoded);

        userRepository.save(userEntity);
    }

    /*
    UserEntity userEntity = UserEntity.builder()
                .name(registerUserRequest.name())
                .email(registerUserRequest.email())
                .password(registerUserRequest.password())
                .role(RoleUser.USER)
                .build();
             USAR NO SERVICE
     */
}
