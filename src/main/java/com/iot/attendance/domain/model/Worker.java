package com.iot.attendance.domain.model;

import com.iot.attendance.domain.enums.WorkerStatus;
import com.iot.attendance.domain.valueobjects.FingerprintId;
import com.iot.attendance.domain.valueobjects.RfidTag;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Worker {

    private Long id;

    @Setter
    private String firstName;

    @Setter
    private String lastName;

    @Setter
    private String documentNumber;

    @Setter
    private String email;

    @Setter
    private String phoneNumber;

    @Setter
    private FingerprintId fingerprintId;

    @Builder.Default
    private Set<RfidTag> rfidTags = new HashSet<>();

    @Setter
    private boolean hasRestrictedAreaAccess;

    @Setter
    @Builder.Default
    private WorkerStatus status = WorkerStatus.ACTIVE;

    private LocalDateTime createdAt;

    @Setter
    private LocalDateTime updatedAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void addRfidTag(RfidTag rfidTag) {
        this.rfidTags.add(rfidTag);
    }

    public void removeRfidTag(RfidTag rfidTag) {
        this.rfidTags.remove(rfidTag);
    }

    public boolean isActive() {
        return this.status == WorkerStatus.ACTIVE;
    }

    public void activate() {
        this.status = WorkerStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = WorkerStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
}