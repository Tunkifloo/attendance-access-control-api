package com.iot.attendance.application.service.impl;

import com.iot.attendance.application.dto.response.SecurityLogResponse;
import com.iot.attendance.application.service.SecurityService;
import com.iot.attendance.infrastructure.persistence.entity.SecurityLogEntity;
import com.iot.attendance.infrastructure.persistence.repository.SecurityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        log.warn("Logging failed access attempt for fingerprint: {}", fingerprintId);

        SecurityLogEntity entity = SecurityLogEntity.builder()
                .eventType("FAILED_ACCESS_ATTEMPT")
                .description("Failed access attempt with unrecognized fingerprint")
                .fingerprintAttempt(String.valueOf(fingerprintId))
                .attemptCount(1)
                .severity("MEDIUM")
                .eventTime(LocalDateTime.now())
                .build();

        securityLogRepository.save(entity);
    }

    @Override
    public void logMultipleFailedAttempts(Integer fingerprintId, int attemptCount) {
        log.error("Multiple failed access attempts detected for fingerprint: {} (Count: {})",
                fingerprintId, attemptCount);

        SecurityLogEntity entity = SecurityLogEntity.builder()
                .eventType("MULTIPLE_FAILED_ATTEMPTS")
                .description(String.format("Alert: %d consecutive failed access attempts detected", attemptCount))
                .fingerprintAttempt(String.valueOf(fingerprintId))
                .attemptCount(attemptCount)
                .severity("HIGH")
                .eventTime(LocalDateTime.now())
                .build();

        securityLogRepository.save(entity);
    }

    @Override
    public void logSecurityEvent(String eventType, String description, String severity) {
        log.info("Logging security event: {} - {}", eventType, description);

        SecurityLogEntity entity = SecurityLogEntity.builder()
                .eventType(eventType)
                .description(description)
                .severity(severity)
                .eventTime(LocalDateTime.now())
                .build();

        securityLogRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityLogResponse> getSecurityLogsByTimeRange(
            LocalDateTime startTime, LocalDateTime endTime) {

        List<SecurityLogEntity> entities = securityLogRepository
                .findByEventTimeBetweenOrderByEventTimeDesc(startTime, endTime);

        return entities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityLogResponse> getCriticalEvents() {
        List<SecurityLogEntity> entities = securityLogRepository.findCriticalEvents();

        return entities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityLogResponse> getRecentEventsByType(String eventType, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<SecurityLogEntity> entities = securityLogRepository
                .findRecentEventsByType(eventType, since);

        return entities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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