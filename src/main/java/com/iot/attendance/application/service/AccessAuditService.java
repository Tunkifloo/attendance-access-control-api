package com.iot.attendance.application.service;

import com.iot.attendance.application.dto.response.AccessLogResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface AccessAuditService {

    void logAccessGranted(Integer fingerprintId, LocalDateTime timestamp);

    void logAccessDenied(Integer fingerprintId, LocalDateTime timestamp);

    List<AccessLogResponse> getAccessHistoryByWorker(Long workerId);

    List<AccessLogResponse> getAccessHistoryByTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status,
            String sortDirection
    );

    List<AccessLogResponse> getRecentDeniedAccess(int hours);

    List<AccessLogResponse> getRecentGrantedAccess(int hours);

    long countDeniedAccessesByWorker(Long workerId, LocalDateTime startTime, LocalDateTime endTime);
}