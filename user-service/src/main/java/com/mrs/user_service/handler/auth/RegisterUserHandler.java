package com.mrs.user_service.handler.auth;

import com.mrs.user_service.model.UserEntity;
import com.mrs.user_service.repository.UserRepository;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.springframework.security.crypto.password.PasswordEncoder;

public class RegisterUserHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    public RegisterUserHandler(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordValidator passwordValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
    }


    public void register(UserEntity userEntity){
        if(userEntity == null) throw new IllegalArgumentException("User can't be null");

        if(userRepository.existsByEmail(userEntity.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        String password = userEntity.getPassword();

        if(isValidPassword(password)){
            throw new IllegalArgumentException("Password is not valid");
        }

        userEntity.setPassword(passwordEncoder.encode(password));

        userRepository.save(userEntity);
    }

    private boolean isValidPassword(String password) {
        RuleResult ruleResult = passwordValidator.validate(new PasswordData(password));

        return ruleResult.isValid();
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
