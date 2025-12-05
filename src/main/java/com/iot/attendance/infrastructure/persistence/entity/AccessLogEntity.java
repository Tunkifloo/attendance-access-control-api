package com.iot.attendance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_logs", indexes = {
        @Index(name = "idx_access_worker_time", columnList = "worker_id, access_time"),
        @Index(name = "idx_access_granted", columnList = "access_granted"),
        @Index(name = "idx_access_status", columnList = "status")
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

    @Column(name = "worker_id", nullable = true)
    private Long workerId;

    @Column(name = "worker_snapshot_name", length = 200)
    private String workerSnapshotName;

    @Column(name = "fingerprint_id")
    private Integer fingerprintId;

    @Column(name = "access_granted", nullable = false)
    private boolean accessGranted;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "access_time", nullable = false)
    private LocalDateTime accessTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}