package com.skishop.user.mapper;

import com.skishop.user.dto.UserRegistrationRequest;
import com.skishop.user.dto.UserResponse;
import com.skishop.user.entity.Role;
import com.skishop.user.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T23:56:17+0900",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21 (Microsoft)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.roleName( userRoleName( user ) );
        userResponse.birthDate( user.getBirthDate() );
        userResponse.createdAt( user.getCreatedAt() );
        userResponse.email( user.getEmail() );
        userResponse.emailVerified( user.getEmailVerified() );
        userResponse.firstName( user.getFirstName() );
        userResponse.gender( user.getGender() );
        userResponse.id( user.getId() );
        userResponse.lastName( user.getLastName() );
        userResponse.phoneNumber( user.getPhoneNumber() );
        userResponse.phoneVerified( user.getPhoneVerified() );
        userResponse.status( user.getStatus() );
        userResponse.updatedAt( user.getUpdatedAt() );

        return userResponse.build();
    }

    @Override
    public User toEntity(UserRegistrationRequest request) {
        if ( request == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.birthDate( request.getBirthDate() );
        user.email( request.getEmail() );
        user.firstName( request.getFirstName() );
        user.gender( request.getGender() );
        user.lastName( request.getLastName() );
        user.phoneNumber( request.getPhoneNumber() );

        return user.build();
    }

    private String userRoleName(User user) {
        if ( user == null ) {
            return null;
        }
        Role role = user.getRole();
        if ( role == null ) {
            return null;
        }
        String name = role.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
