package com.iot.attendance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "rfid_cards")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RfidCardEntity {

    @Id
    @Column(name = "rfid_uid", nullable = false, unique = true, length = 20)
    private String uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private WorkerEntity worker;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}