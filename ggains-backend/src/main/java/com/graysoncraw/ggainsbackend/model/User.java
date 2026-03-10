package com.graysoncraw.ggainsbackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "firebase_uid", nullable = false, unique = true)
    private String firebaseUid;

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "First name is required")
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Last name is required")
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Column(name = "date_created", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime dateCreated;

    @Column(name = "gender")
    private String gender;

    @Column(name = "height_ft")
    @Min(value = 0, message = "Height in feet must be positive")
    private Integer heightFt;

    @Column(name = "height_in")
    @Min(value = 0, message = "Height in inches must be positive")
    @Max(value = 11, message = "Height in inches must be less than 12")
    private Integer heightIn;

    @Column(name = "weight")
    @Min(value = 0, message = "Weight must be positive")
    private Double weight;

    // Relationships
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private PersonalRecord personalRecord;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private WorkoutSchedule workoutSchedule;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutCycle> workoutCycles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutSession> workoutSessions;
}
