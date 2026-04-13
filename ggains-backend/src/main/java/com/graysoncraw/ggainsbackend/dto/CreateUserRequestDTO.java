package com.graysoncraw.ggainsbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequestDTO {

    @NotBlank(message = "Firebase UID is required")
    private String firebaseUid;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    private String gender;

    @Min(value = 0, message = "Height in feet must be positive")
    private Integer heightFt;

    @Min(value = 0, message = "Height in inches must be positive")
    @Max(value = 11, message = "Height in inches must be less than 12")
    private Integer heightIn;

    @Min(value = 0, message = "Weight must be positive")
    private Double weight;
}
