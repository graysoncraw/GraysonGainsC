package com.graysoncraw.ggainsbackend.mapper;

import com.graysoncraw.ggainsbackend.dto.workoutcycle.WorkoutCycleResponseDTO;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkoutCycleMapper {
    @Mapping(target = "firebaseUid", source = "user.firebaseUid")
    WorkoutCycleResponseDTO toResponse(WorkoutCycle workoutCycle);
}
