package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.PersonalRecordRequestDTO;
import com.graysoncraw.ggainsbackend.dto.PersonalRecordResponseDTO;
import com.graysoncraw.ggainsbackend.mapper.PersonalRecordMapper;
import com.graysoncraw.ggainsbackend.model.PersonalRecord;
import com.graysoncraw.ggainsbackend.service.PersonalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{firebaseUid}/personal-record")
@RequiredArgsConstructor
public class PersonalRecordController {

    private final PersonalRecordService personalRecordService;
    private final PersonalRecordMapper personalRecordMapper;

    @PostMapping
    public ResponseEntity<PersonalRecordResponseDTO> createPersonalRecord(
            @PathVariable String firebaseUid,
            @Valid @RequestBody PersonalRecordRequestDTO request
    ) {
        PersonalRecord personalRecord = personalRecordMapper.toEntity(request);

        PersonalRecord createdRecord = personalRecordService.createPersonalRecord(firebaseUid, personalRecord);
        return ResponseEntity.status(HttpStatus.CREATED).body(personalRecordMapper.toResponse(createdRecord));
    }

    @GetMapping
    public PersonalRecordResponseDTO getPersonalRecord(@PathVariable String firebaseUid) {
        PersonalRecord record = personalRecordService.getPersonalRecordByUserFirebaseUid(firebaseUid);
        return personalRecordMapper.toResponse(record);
    }

    @PutMapping
    public PersonalRecordResponseDTO updatePersonalRecord(
            @PathVariable String firebaseUid,
            @Valid @RequestBody PersonalRecordRequestDTO request
    ) {
        PersonalRecord existingRecord = personalRecordService.getPersonalRecordByUserFirebaseUid(firebaseUid);
        PersonalRecord personalRecord = personalRecordMapper.toEntity(request);
        personalRecord.setId(existingRecord.getId());
        personalRecord.setUser(existingRecord.getUser());

        PersonalRecord updatedRecord = personalRecordService.updatePersonalRecord(personalRecord);
        return personalRecordMapper.toResponse(updatedRecord);
    }

    @PatchMapping("/{liftType}")
    public PersonalRecordResponseDTO updateSpecificPr(
            @PathVariable String firebaseUid,
            @PathVariable String liftType,
            @RequestParam Double newPR
    ) {
        PersonalRecord updatedRecord = personalRecordService.updateSpecificPR(firebaseUid, liftType, newPR);
        return personalRecordMapper.toResponse(updatedRecord);
    }
}
