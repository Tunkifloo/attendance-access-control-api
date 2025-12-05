package com.iot.attendance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_logs", indexes = {
        @Index(name = "idx_security_event_time", columnList = "event_time"),
        @Index(name = "idx_security_severity", columnList = "severity")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SecurityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "fingerprint_attempt", length = 50)
    private String fingerprintAttempt;

    @Column(name = "attempt_count")
    private Integer attemptCount;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}