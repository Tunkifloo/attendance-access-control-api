package com.iot.attendance.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessLog {

    private Long id;

    private Long workerId;

    private Integer fingerprintId;

    private boolean accessGranted;

    private String location;

    private String status;

    private LocalDateTime accessTime;

    private LocalDateTime createdAt;

    public boolean wasGranted() {
        return this.accessGranted;
    }

    public boolean wasDenied() {
        return !this.accessGranted;
    }
}