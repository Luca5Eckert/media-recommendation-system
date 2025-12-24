package com.mrs.user_service.handler.user_preference;

import com.mrs.user_service.model.UserPreference;
import com.mrs.user_service.repository.UserPreferenceRepository;
import com.mrs.user_service.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateUserPrefenceHandler {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;

    public CreateUserPrefenceHandler(UserPreferenceRepository userPreferenceRepository, UserRepository userRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void execute(UserPreference userPreference){
        if(!userRepository.existsById(userPreference.getUserId())) throw new IllegalArgumentException("User not found");

        // Race condition //
        if(userPreferenceRepository.existsByUserId(userPreference.getUserId())) throw new IllegalArgumentException("User already have a preference");

        userPreferenceRepository.save(userPreference);
    }


}
