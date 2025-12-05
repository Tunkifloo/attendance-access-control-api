package com.iot.attendance.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityLog {

    private Long id;

    private String eventType;

    private String description;

    private String fingerprintAttempt;

    private Integer attemptCount;

    private LocalDateTime eventTime;

    private LocalDateTime createdAt;
}