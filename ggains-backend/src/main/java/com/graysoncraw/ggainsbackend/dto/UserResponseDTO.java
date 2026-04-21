package com.graysoncraw.ggainsbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDTO {
    private String firebaseUid;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDateTime dateCreated;
    private String gender;
    private Integer heightFt;
    private Integer heightIn;
    private Double weight;
}
