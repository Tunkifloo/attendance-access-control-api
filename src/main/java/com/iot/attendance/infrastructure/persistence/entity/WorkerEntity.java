package com.iot.attendance.infrastructure.persistence.entity;

import com.iot.attendance.domain.enums.WorkerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "workers", indexes = {
        @Index(name = "idx_worker_document", columnList = "document_number"),
        @Index(name = "idx_worker_fingerprint", columnList = "fingerprint_id"),
        @Index(name = "idx_worker_status", columnList = "status")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "document_number", unique = true, nullable = false, length = 20)
    private String documentNumber;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "fingerprint_id", unique = true)
    private Integer fingerprintId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "worker_rfid_tags",
            joinColumns = @JoinColumn(name = "worker_id"),
            indexes = @Index(name = "idx_rfid_uid", columnList = "rfid_uid")
    )
    @Column(name = "rfid_uid", length = 50)
    @Builder.Default
    private Set<String> rfidTags = new HashSet<>();

    @Column(name = "has_restricted_area_access", nullable = false)
    @Builder.Default
    private boolean hasRestrictedAreaAccess = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WorkerStatus status = WorkerStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}