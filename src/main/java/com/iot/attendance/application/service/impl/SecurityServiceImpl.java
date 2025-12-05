package com.iot.attendance.application.service.impl;

import com.iot.attendance.application.dto.response.SecurityLogResponse;
import com.iot.attendance.application.service.SecurityService;
import com.iot.attendance.infrastructure.persistence.entity.SecurityLogEntity;
import com.iot.attendance.infrastructure.persistence.repository.SecurityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SecurityServiceImpl implements SecurityService {

    private final SecurityLogRepository securityLogRepository;

    @Override
    public void logFailedAccessAttempt(Integer fingerprintId) {
        logSecurityEvent(
                "FAILED_ACCESS_ATTEMPT",
                "Failed access attempt with unrecognized fingerprint: " + fingerprintId,
                "ACCESS"
        );
    }

    @Override
    public void logSecurityEvent(String eventType, String description, String severity) {
        log.info("Logging security event [{}]: {} - {}", severity, eventType, description);

        SecurityLogEntity entity = SecurityLogEntity.builder()
                .eventType(eventType)
                .description(description)
                .severity(severity.toUpperCase())
                .eventTime(LocalDateTime.now())
                .build();

        securityLogRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityLogResponse> getSecurityLogsByTimeRange(
            LocalDateTime startTime, LocalDateTime endTime, String severity, String sortDirection) {

        Sort sort = Sort.by("eventTime");
        if ("ASC".equalsIgnoreCase(sortDirection)) {
            sort = sort.ascending();
        } else {
            sort = sort.descending();
        }

        List<SecurityLogEntity> entities;
        if (severity != null && !severity.isEmpty() && !severity.equals("ALL")) {
            entities = securityLogRepository.findByEventTimeBetweenAndSeverity(startTime, endTime, severity, sort);
        } else {
            entities = securityLogRepository.findByEventTimeBetween(startTime, endTime, sort);
        }

        return entities.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private SecurityLogResponse mapToResponse(SecurityLogEntity entity) {
        return SecurityLogResponse.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .description(entity.getDescription())
                .fingerprintAttempt(entity.getFingerprintAttempt())
                .attemptCount(entity.getAttemptCount())
                .severity(entity.getSeverity())
                .eventTime(entity.getEventTime())
                .build();
    }
}