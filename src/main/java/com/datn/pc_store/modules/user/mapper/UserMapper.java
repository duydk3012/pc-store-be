package com.datn.pc_store.modules.user.mapper;

import com.datn.pc_store.modules.user.dto.*;
import com.datn.pc_store.modules.user.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "username", source = "username")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "citizenIdentificationCard", source = "citizenIdentificationCard")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "status", source = "status")
    User toEntity(UserCreateRequest req);

    UserResponse toResponse(User entity);

    List<UserResponse> toResponseList(List<User> entities);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "username", source = "username")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "citizenIdentificationCard", source = "citizenIdentificationCard")
    void updateEntityFromDto(UserUpdateRequest req, @MappingTarget User entity);
}
