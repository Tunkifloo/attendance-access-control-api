package com.iot.attendance.application.service;

import com.iot.attendance.application.dto.request.FingerprintAccessRequest;
import com.iot.attendance.application.dto.response.AccessLogResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface AccessControlService {

    AccessLogResponse processAccess(FingerprintAccessRequest request);

    AccessLogResponse getAccessLogById(Long id);

    List<AccessLogResponse> getAccessLogsByWorker(Long workerId);

    List<AccessLogResponse> getAccessLogsByTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    List<AccessLogResponse> getRecentDeniedAccess(int hours);
}