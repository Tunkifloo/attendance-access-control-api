package com.iot.attendance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "system_configuration")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_start_time", nullable = false)
    private LocalTime workStartTime;

    @Column(name = "work_end_time", nullable = false)
    private LocalTime workEndTime;

    @Column(name = "late_threshold_minutes", nullable = false)
    private Integer lateThresholdMinutes;

    @Column(name = "current_attendance_date")
    private LocalDate currentAttendanceDate;

    @Column(name = "simulated_date_time")
    private LocalDateTime simulatedDateTime;

    @Column(name = "simulation_mode", nullable = false)
    @Builder.Default
    private boolean simulationMode = false;

    @Column(name = "max_failed_access_attempts", nullable = false)
    @Builder.Default
    private Integer maxFailedAccessAttempts = 3;

    @Column(name = "alert_cooldown_minutes", nullable = false)
    @Builder.Default
    private Integer alertCooldownMinutes = 5;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}