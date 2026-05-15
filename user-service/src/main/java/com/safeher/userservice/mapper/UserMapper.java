package com.safeher.userservice.mapper;

import com.safeher.userservice.dto.request.CreateUserRequest;
import com.safeher.userservice.dto.request.UpdateUserRequest;
import com.safeher.userservice.dto.response.PublicUserResponse;
import com.safeher.userservice.dto.response.UserResponse;
import com.safeher.userservice.entity.User;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "role",        ignore = true)
    @Mapping(target = "active",      ignore = true)
    @Mapping(target = "verified",    ignore = true)
    @Mapping(target = "totalReviews",ignore = true)
    @Mapping(target = "helpfulVotes",ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    @Mapping(target = "lastSeenAt",  ignore = true)
    @Mapping(target = "anonymous",   ignore = true)
    User toEntity(CreateUserRequest request);

    UserResponse toResponse(User user);

    PublicUserResponse toPublicResponse(User user);

    /**
     * Partial update: only non-null fields from the request are applied.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "authUserId",  ignore = true)
    @Mapping(target = "username",    ignore = true)
    @Mapping(target = "email",       ignore = true)
    @Mapping(target = "role",        ignore = true)
    @Mapping(target = "active",      ignore = true)
    @Mapping(target = "verified",    ignore = true)
    @Mapping(target = "totalReviews",ignore = true)
    @Mapping(target = "helpfulVotes",ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    @Mapping(target = "lastSeenAt",  ignore = true)
    @Mapping(target = "avatarUrl",   ignore = true)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);
}
