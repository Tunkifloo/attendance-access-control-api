package com.iot.attendance.application.service;

import com.iot.attendance.application.dto.response.SecurityLogResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface SecurityService {

    void logFailedAccessAttempt(Integer fingerprintId);

    void logMultipleFailedAttempts(Integer fingerprintId, int attemptCount);

    void logSecurityEvent(String eventType, String description, String severity);

    List<SecurityLogResponse> getSecurityLogsByTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    List<SecurityLogResponse> getCriticalEvents();

    List<SecurityLogResponse> getRecentEventsByType(String eventType, int hours);
}