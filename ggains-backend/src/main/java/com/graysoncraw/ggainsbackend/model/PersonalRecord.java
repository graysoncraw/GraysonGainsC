package com.graysoncraw.ggainsbackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "personal_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "firebase_uid", nullable = false, unique = true)
    private User user;

    @Column(name = "bench_press_pr", nullable = false)
    @NotNull(message = "Bench press PR is required")
    @Min(value = 0, message = "Bench press PR must be positive")
    private Double benchPressPR;

    @Column(name = "squat_pr", nullable = false)
    @NotNull(message = "Squat PR is required")
    @Min(value = 0, message = "Squat PR must be positive")
    private Double squatPR;

    @Column(name = "deadlift_pr", nullable = false)
    @NotNull(message = "Deadlift PR is required")
    @Min(value = 0, message = "Deadlift PR must be positive")
    private Double deadliftPR;

    @Column(name = "shoulder_press_pr", nullable = false)
    @NotNull(message = "Shoulder press PR is required")
    @Min(value = 0, message = "Shoulder press PR must be positive")
    private Double shoulderPressPR;
}
