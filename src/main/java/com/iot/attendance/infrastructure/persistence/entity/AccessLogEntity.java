package com.iot.attendance.infrastructure.persistence.entity;

import com.iot.attendance.domain.enums.AccessStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_logs", indexes = {
        @Index(name = "idx_access_worker_time", columnList = "worker_id, access_time"),
        @Index(name = "idx_access_status", columnList = "status"),
        @Index(name = "idx_access_time", columnList = "access_time")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccessLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id")
    private Long workerId;

    @Column(name = "fingerprint_id")
    private Integer fingerprintId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccessStatus status;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "denial_reason", length = 255)
    private String denialReason;

    @Column(name = "access_time", nullable = false)
    private LocalDateTime accessTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}