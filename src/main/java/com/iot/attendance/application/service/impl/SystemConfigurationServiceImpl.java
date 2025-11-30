package com.iot.attendance.application.service.impl;

import com.iot.attendance.application.dto.request.UpdateSystemConfigRequest;
import com.iot.attendance.application.dto.response.SystemConfigurationResponse;
import com.iot.attendance.application.service.SystemConfigurationService;
import com.iot.attendance.infrastructure.exception.ResourceNotFoundException;
import com.iot.attendance.infrastructure.persistence.entity.SystemConfigurationEntity;
import com.iot.attendance.infrastructure.persistence.repository.SystemConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SystemConfigurationServiceImpl implements SystemConfigurationService {

    private final SystemConfigurationRepository configRepository;

    @Value("${attendance.work-start-time}")
    private String defaultWorkStartTime;

    @Value("${attendance.late-threshold-minutes}")
    private Integer defaultLateThreshold;

    @Value("${access.max-failed-attempts}")
    private Integer defaultMaxFailedAttempts;

    @Value("${access.alert-cooldown-minutes}")
    private Integer defaultAlertCooldown;

    @Override
    @Transactional(readOnly = true)
    public SystemConfigurationResponse getCurrentConfiguration() {
        SystemConfigurationEntity entity = configRepository.findLatestConfiguration()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "System configuration not found. Please initialize the system first."
                ));

        return mapToResponse(entity);
    }

    @Override
    public SystemConfigurationResponse updateConfiguration(UpdateSystemConfigRequest request) {
        log.info("Updating system configuration");

        SystemConfigurationEntity entity = configRepository.findLatestConfiguration()
                .orElseGet(this::createDefaultEntity);

        if (request.getWorkStartTime() != null) {
            entity.setWorkStartTime(request.getWorkStartTime());
        }
        if (request.getWorkEndTime() != null) {
            entity.setWorkEndTime(request.getWorkEndTime());
        }
        if (request.getLateThresholdMinutes() != null) {
            entity.setLateThresholdMinutes(request.getLateThresholdMinutes());
        }
        if (request.getCurrentAttendanceDate() != null) {
            entity.setCurrentAttendanceDate(request.getCurrentAttendanceDate());
        }
        if (request.getSimulatedDateTime() != null) {
            entity.setSimulatedDateTime(request.getSimulatedDateTime());
        }
        if (request.getSimulationMode() != null) {
            entity.setSimulationMode(request.getSimulationMode());
        }
        if (request.getMaxFailedAccessAttempts() != null) {
            entity.setMaxFailedAccessAttempts(request.getMaxFailedAccessAttempts());
        }
        if (request.getAlertCooldownMinutes() != null) {
            entity.setAlertCooldownMinutes(request.getAlertCooldownMinutes());
        }

        entity.setUpdatedAt(LocalDateTime.now());
        SystemConfigurationEntity updated = configRepository.save(entity);

        log.info("System configuration updated successfully");
        return mapToResponse(updated);
    }

    @Override
    public SystemConfigurationResponse initializeDefaultConfiguration() {
        log.info("Initializing default system configuration");

        SystemConfigurationEntity entity = createDefaultEntity();
        SystemConfigurationEntity saved = configRepository.save(entity);

        log.info("Default configuration initialized successfully");
        return mapToResponse(saved);
    }

    @Override
    public SystemConfigurationResponse enableSimulationMode(UpdateSystemConfigRequest request) {
        log.info("Enabling simulation mode");

        SystemConfigurationEntity entity = configRepository.findLatestConfiguration()
                .orElseGet(this::createDefaultEntity);

        entity.setSimulationMode(true);

        if (request.getSimulatedDateTime() != null) {
            entity.setSimulatedDateTime(request.getSimulatedDateTime());
        }
        if (request.getCurrentAttendanceDate() != null) {
            entity.setCurrentAttendanceDate(request.getCurrentAttendanceDate());
        }

        entity.setUpdatedAt(LocalDateTime.now());
        SystemConfigurationEntity updated = configRepository.save(entity);

        log.info("Simulation mode enabled");
        return mapToResponse(updated);
    }

    @Override
    public SystemConfigurationResponse disableSimulationMode() {
        log.info("Disabling simulation mode");

        SystemConfigurationEntity entity = configRepository.findLatestConfiguration()
                .orElseThrow(() -> new ResourceNotFoundException("System configuration not found"));

        entity.setSimulationMode(false);
        entity.setSimulatedDateTime(null);
        entity.setCurrentAttendanceDate(null);
        entity.setUpdatedAt(LocalDateTime.now());

        SystemConfigurationEntity updated = configRepository.save(entity);

        log.info("Simulation mode disabled");
        return mapToResponse(updated);
    }

    private SystemConfigurationEntity createDefaultEntity() {
        return SystemConfigurationEntity.builder()
                .workStartTime(LocalTime.parse(defaultWorkStartTime))
                .workEndTime(LocalTime.parse("17:00:00"))
                .lateThresholdMinutes(defaultLateThreshold)
                .simulationMode(false)
                .maxFailedAccessAttempts(defaultMaxFailedAttempts)
                .alertCooldownMinutes(defaultAlertCooldown)
                .build();
    }

    private SystemConfigurationResponse mapToResponse(SystemConfigurationEntity entity) {
        return SystemConfigurationResponse.builder()
                .id(entity.getId())
                .workStartTime(entity.getWorkStartTime())
                .workEndTime(entity.getWorkEndTime())
                .lateThresholdMinutes(entity.getLateThresholdMinutes())
                .currentAttendanceDate(entity.getCurrentAttendanceDate())
                .simulatedDateTime(entity.getSimulatedDateTime())
                .simulationMode(entity.isSimulationMode())
                .maxFailedAccessAttempts(entity.getMaxFailedAccessAttempts())
                .alertCooldownMinutes(entity.getAlertCooldownMinutes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}