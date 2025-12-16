package com.mrs.user_service.mapper;

import com.mrs.user_service.dto.UserGetResponse;
import com.mrs.user_service.model.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserGetResponse toGetResponse(UserEntity user){
        return new UserGetResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

}
