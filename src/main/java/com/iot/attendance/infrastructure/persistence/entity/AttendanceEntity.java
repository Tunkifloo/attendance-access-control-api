package com.iot.attendance.infrastructure.persistence.entity;

import com.iot.attendance.domain.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendances", indexes = {
        @Index(name = "idx_attendance_worker_date", columnList = "worker_id, attendance_date"),
        @Index(name = "idx_attendance_date", columnList = "attendance_date"),
        @Index(name = "idx_attendance_status", columnList = "status")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", nullable = false)
    private Long workerId;

    @Column(name = "rfid_tag", nullable = false, length = 50)
    private String rfidTag;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "worked_duration_seconds")
    private Long workedDurationSeconds;

    @Column(name = "is_late", nullable = false)
    @Builder.Default
    private boolean isLate = false;

    @Column(name = "lateness_duration_seconds")
    private Long latenessDurationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.CHECKED_IN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public Duration getWorkedDuration() {
        return workedDurationSeconds != null ? Duration.ofSeconds(workedDurationSeconds) : null;
    }

    public void setWorkedDuration(Duration duration) {
        this.workedDurationSeconds = duration != null ? duration.getSeconds() : null;
    }

    public Duration getLatenessDuration() {
        return latenessDurationSeconds != null ? Duration.ofSeconds(latenessDurationSeconds) : null;
    }

    public void setLatenessDuration(Duration duration) {
        this.latenessDurationSeconds = duration != null ? duration.getSeconds() : null;
    }
}
