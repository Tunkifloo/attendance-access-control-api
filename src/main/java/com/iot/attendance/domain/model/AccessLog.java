package com.iot.attendance.domain.model;

import com.iot.attendance.domain.enums.AccessStatus;
import com.iot.attendance.domain.valueobjects.FingerprintId;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessLog {

    private Long id;

    private Long workerId;

    private FingerprintId fingerprintId;

    private AccessStatus status;

    private String location;

    private String denialReason;

    private LocalDateTime accessTime;

    private LocalDateTime createdAt;

    public boolean isGranted() {
        return this.status == AccessStatus.GRANTED;
    }

    public boolean isDenied() {
        return this.status == AccessStatus.DENIED;
    }
}