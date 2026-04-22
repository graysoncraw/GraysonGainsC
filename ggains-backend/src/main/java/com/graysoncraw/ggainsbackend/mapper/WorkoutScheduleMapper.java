package com.graysoncraw.ggainsbackend.mapper;

import com.graysoncraw.ggainsbackend.dto.WorkoutScheduleRequestDTO;
import com.graysoncraw.ggainsbackend.dto.WorkoutScheduleResponseDTO;
import com.graysoncraw.ggainsbackend.model.WorkoutSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkoutScheduleMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    WorkoutSchedule toEntity(WorkoutScheduleRequestDTO request);

    @Mapping(target = "firebaseUid", source = "user.firebaseUid")
    WorkoutScheduleResponseDTO toResponse(WorkoutSchedule workoutSchedule);
}
