package com.mrs.user_service.handler;

import com.mrs.user_service.dto.PageUser;
import com.mrs.user_service.model.UserEntity;
import com.mrs.user_service.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class GetAllUserHandler {

    private final UserRepository userRepository;

    public GetAllUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<UserEntity> execute(
            PageUser pageUser
    ) {

        PageRequest pageRequest = PageRequest.of(
                pageUser.pageNumber(),
                pageUser.pageSize()
        );

        return userRepository.findAll(pageRequest);
    }


}
