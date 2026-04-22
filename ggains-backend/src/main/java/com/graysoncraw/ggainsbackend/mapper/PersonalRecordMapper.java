package com.graysoncraw.ggainsbackend.mapper;

import com.graysoncraw.ggainsbackend.dto.PersonalRecordRequestDTO;
import com.graysoncraw.ggainsbackend.dto.PersonalRecordResponseDTO;
import com.graysoncraw.ggainsbackend.model.PersonalRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PersonalRecordMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    PersonalRecord toEntity(PersonalRecordRequestDTO request);

    @Mapping(target = "firebaseUid", source = "user.firebaseUid")
    PersonalRecordResponseDTO toResponse(PersonalRecord personalRecord);
}
