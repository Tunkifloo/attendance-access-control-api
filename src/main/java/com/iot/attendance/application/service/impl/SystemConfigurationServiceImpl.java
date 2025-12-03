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

    // Valores por defecto desde application.yml
    @Value("${attendance.work-start-time}")
    private String defaultWorkStartTime;

    @Value("${attendance.work-end-time:17:00:00}")
    private String defaultWorkEndTime;

    @Value("${attendance.late-threshold-minutes}")
    private Integer defaultLateThreshold;

    @Override
    @Transactional(readOnly = true)
    public SystemConfigurationResponse getCurrentConfiguration() {
        log.info("Fetching current system configuration");

        SystemConfigurationEntity entity = configRepository.findLatestConfiguration()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "System configuration not found. Please initialize the system first."
                ));

        log.info("✓ Configuration found: Work hours {} - {}, Late threshold: {} min, Simulation: {}",
                entity.getWorkStartTime(),
                entity.getWorkEndTime(),
                entity.getLateThresholdMinutes(),
                entity.isSimulationMode() ? "ENABLED" : "DISABLED");

        return mapToResponse(entity);
    }

    @Override
    public SystemConfigurationResponse updateConfiguration(UpdateSystemConfigRequest request) {
        log.info("Updating system configuration");

        SystemConfigurationEntity entity = configRepository.findLatestConfiguration()
                .orElseGet(() -> {
                    log.warn("Configuration not found, creating default");
                    return createDefaultEntity();
                });

        // Guardar valores anteriores para log
        LocalTime oldStartTime = entity.getWorkStartTime();
        LocalTime oldEndTime = entity.getWorkEndTime();
        Integer oldThreshold = entity.getLateThresholdMinutes();

        // Actualizar solo campos proporcionados (PATCH semántico)
        if (request.getWorkStartTime() != null) {
            entity.setWorkStartTime(request.getWorkStartTime());
            log.info("  → Work start time: {} → {}", oldStartTime, request.getWorkStartTime());
        }

        if (request.getWorkEndTime() != null) {
            entity.setWorkEndTime(request.getWorkEndTime());
            log.info("  → Work end time: {} → {}", oldEndTime, request.getWorkEndTime());
        }

        if (request.getLateThresholdMinutes() != null) {
            entity.setLateThresholdMinutes(request.getLateThresholdMinutes());
            log.info("  → Late threshold: {} min → {} min", oldThreshold, request.getLateThresholdMinutes());
        }

        // Campos de simulación
        if (request.getSimulatedDate() != null) {
            entity.setSimulatedDate(request.getSimulatedDate());
            log.info("  → Simulated date set to: {}", request.getSimulatedDate());
        }

        if (request.getSimulatedDateTime() != null) {
            entity.setSimulatedDateTime(request.getSimulatedDateTime());
            // También actualizar simulatedDate para consistencia
            entity.setSimulatedDate(request.getSimulatedDateTime().toLocalDate());
            log.info("  → Simulated date/time set to: {}", request.getSimulatedDateTime());
        }

        if (request.getSimulationMode() != null) {
            entity.setSimulationMode(request.getSimulationMode());
            log.info("  → Simulation mode: {}", request.getSimulationMode() ? "ENABLED" : "DISABLED");

            // Si se desactiva simulación, limpiar campos simulados
            if (!request.getSimulationMode()) {
                entity.setSimulatedDate(null);
                entity.setSimulatedDateTime(null);
                log.info("  → Simulated values cleared (simulation disabled)");
            }
        }

        entity.setUpdatedAt(LocalDateTime.now());
        SystemConfigurationEntity updated = configRepository.save(entity);

        log.info("✓ System configuration updated successfully");
        return mapToResponse(updated);
    }

    @Override
    public SystemConfigurationResponse initializeDefaultConfiguration() {
        log.info("Initializing default system configuration");

        // Verificar si ya existe configuración
        if (configRepository.findLatestConfiguration().isPresent()) {
            log.warn("⚠ Configuration already exists, returning existing config");
            return getCurrentConfiguration();
        }

        SystemConfigurationEntity entity = createDefaultEntity();
        SystemConfigurationEntity saved = configRepository.save(entity);

        log.info("✓ Default configuration initialized successfully");
        log.info("  → Work hours: {} - {}", saved.getWorkStartTime(), saved.getWorkEndTime());
        log.info("  → Late threshold: {} minutes", saved.getLateThresholdMinutes());
        log.info("  → Simulation mode: DISABLED");

        return mapToResponse(saved);
    }

    @Override
    public SystemConfigurationResponse enableSimulationMode(UpdateSystemConfigRequest request) {
        log.info("Enabling simulation mode");

        SystemConfigurationEntity entity = configRepository.findLatestConfiguration()
                .orElseGet(() -> {
                    log.warn("Configuration not found, creating default before enabling simulation");
                    return createDefaultEntity();
                });

        entity.setSimulationMode(true);

        // Prioridad: simulatedDateTime sobre simulatedDate
        if (request.getSimulatedDateTime() != null) {
            entity.setSimulatedDateTime(request.getSimulatedDateTime());
            entity.setSimulatedDate(request.getSimulatedDateTime().toLocalDate());
            log.info("  → Simulated date/time: {}", request.getSimulatedDateTime());
        } else if (request.getSimulatedDate() != null) {
            entity.setSimulatedDate(request.getSimulatedDate());
            // Limpiar simulatedDateTime para evitar inconsistencias
            entity.setSimulatedDateTime(null);
            log.info("  → Simulated date: {}", request.getSimulatedDate());
        } else {
            log.warn("  ⚠ No simulated date/time provided, simulation enabled but using real time");
        }

        entity.setUpdatedAt(LocalDateTime.now());
        SystemConfigurationEntity updated = configRepository.save(entity);

        log.info("✓ Simulation mode ENABLED");
        return mapToResponse(updated);
    }

    @Override
    public SystemConfigurationResponse disableSimulationMode() {
        log.info("Disabling simulation mode");

        SystemConfigurationEntity entity = configRepository.findLatestConfiguration()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "System configuration not found. Cannot disable simulation."
                ));

        if (!entity.isSimulationMode()) {
            log.info("⚠ Simulation mode already disabled");
            return mapToResponse(entity);
        }

        entity.setSimulationMode(false);
        entity.setSimulatedDateTime(null);
        entity.setSimulatedDate(null);
        entity.setUpdatedAt(LocalDateTime.now());

        SystemConfigurationEntity updated = configRepository.save(entity);

        log.info("✓ Simulation mode DISABLED - Using real system time");
        return mapToResponse(updated);
    }

    private SystemConfigurationEntity createDefaultEntity() {
        return SystemConfigurationEntity.builder()
                .workStartTime(LocalTime.parse(defaultWorkStartTime))
                .workEndTime(LocalTime.parse(defaultWorkEndTime))
                .lateThresholdMinutes(defaultLateThreshold)
                .simulationMode(false)
                .simulatedDate(null)
                .simulatedDateTime(null)
                .build();
    }

    private SystemConfigurationResponse mapToResponse(SystemConfigurationEntity entity) {
        return SystemConfigurationResponse.builder()
                .id(entity.getId())
                .workStartTime(entity.getWorkStartTime())
                .workEndTime(entity.getWorkEndTime())
                .lateThresholdMinutes(entity.getLateThresholdMinutes())
                .simulatedDate(entity.getSimulatedDate())
                .simulatedDateTime(entity.getSimulatedDateTime())
                .simulationMode(entity.isSimulationMode())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}