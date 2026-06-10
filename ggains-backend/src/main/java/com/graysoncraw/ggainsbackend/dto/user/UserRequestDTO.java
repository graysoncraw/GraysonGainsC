package com.graysoncraw.ggainsbackend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UserRequestDTO {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(Male|Female)$", message = "Gender must be Male or Female")
    private String gender;

    @NotNull(message = "Height in feet is required")
    @Min(value = 0, message = "Height in feet must be positive")
    private Integer heightFt;

    @NotNull(message = "Height in inches is required")
    @Min(value = 0, message = "Height in inches must be positive")
    @Max(value = 11, message = "Height in inches must be less than 12")
    private Integer heightIn;

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be greater than 0")
    private Double weight;
}
