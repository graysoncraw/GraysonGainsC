package com.graysoncraw.ggainsbackend.mapper;

import com.graysoncraw.ggainsbackend.dto.UserRequestDTO;
import com.graysoncraw.ggainsbackend.dto.UserResponseDTO;
import com.graysoncraw.ggainsbackend.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mappings({
            @Mapping(target = "dateCreated", ignore = true),
            @Mapping(target = "personalRecord", ignore = true),
            @Mapping(target = "workoutSchedule", ignore = true),
            @Mapping(target = "workoutCycles", ignore = true),
            @Mapping(target = "workoutSessions", ignore = true)
    })
    User toEntity(UserRequestDTO request);

    UserResponseDTO toResponse(User user);
}
