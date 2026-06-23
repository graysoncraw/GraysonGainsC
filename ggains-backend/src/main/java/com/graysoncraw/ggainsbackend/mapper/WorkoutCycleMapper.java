package com.graysoncraw.ggainsbackend.mapper;

import com.graysoncraw.ggainsbackend.dto.workoutcycle.WorkoutCycleRequestDTO;
import com.graysoncraw.ggainsbackend.dto.workoutcycle.WorkoutCycleResponseDTO;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkoutCycleMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "benchDay", source = "benchDay")
    @Mapping(target = "squatDay", source = "squatDay")
    @Mapping(target = "deadliftDay", source = "deadliftDay")
    @Mapping(target = "shoulderPressDay", source = "shoulderPressDay")
    WorkoutCycle toEntity(WorkoutCycleRequestDTO request);

    @Mapping(target = "firebaseUid", source = "user.firebaseUid")
    WorkoutCycleResponseDTO toResponse(WorkoutCycle workoutCycle);
}
