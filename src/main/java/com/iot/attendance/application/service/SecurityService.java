package com.iot.attendance.application.service;

import com.iot.attendance.application.dto.response.SecurityLogResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface SecurityService {

    void logFailedAccessAttempt(Integer fingerprintId);

    void logSecurityEvent(String eventType, String description, String severity);

    List<SecurityLogResponse> getSecurityLogsByTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String severity,
            String sortDirection
    );
}