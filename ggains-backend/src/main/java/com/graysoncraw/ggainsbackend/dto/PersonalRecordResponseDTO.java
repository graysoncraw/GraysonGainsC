package com.graysoncraw.ggainsbackend.dto;

import lombok.Data;

@Data
public class PersonalRecordResponseDTO {
    private Long id;
    private String firebaseUid;
    private Double benchPressPR;
    private Double squatPR;
    private Double deadliftPR;
    private Double shoulderPressPR;
}
