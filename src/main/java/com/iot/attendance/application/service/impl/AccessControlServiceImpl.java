package com.iot.attendance.application.service.impl;

import com.iot.attendance.application.dto.request.FingerprintAccessRequest;
import com.iot.attendance.application.dto.response.AccessLogResponse;
import com.iot.attendance.application.mapper.AccessLogMapper;
import com.iot.attendance.application.service.AccessControlService;
import com.iot.attendance.application.service.SecurityService;
import com.iot.attendance.domain.enums.AccessStatus;
import com.iot.attendance.domain.model.AccessLog;
import com.iot.attendance.infrastructure.exception.ResourceNotFoundException;
import com.iot.attendance.infrastructure.firebase.FirebaseRealtimeService;
import com.iot.attendance.infrastructure.persistence.entity.AccessLogEntity;
import com.iot.attendance.infrastructure.persistence.entity.SystemConfigurationEntity;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import com.iot.attendance.infrastructure.persistence.repository.AccessLogRepository;
import com.iot.attendance.infrastructure.persistence.repository.SystemConfigurationRepository;
import com.iot.attendance.infrastructure.persistence.repository.WorkerRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccessControlServiceImpl implements AccessControlService {

    private final AccessLogRepository accessLogRepository;
    private final WorkerRepository workerRepository;
    private final SystemConfigurationRepository configRepository;

    @Getter
    private final AccessLogMapper accessLogMapper;
    private final SecurityService securityService;
    private final FirebaseRealtimeService firebaseService;

    @Override
    public AccessLogResponse processAccess(FingerprintAccessRequest request) {
        log.info("Processing access request for fingerprint ID: {}", request.getFingerprintId());

        SystemConfigurationEntity config = getCurrentConfiguration();
        LocalDateTime accessTime = request.getTimestamp() != null ?
                request.getTimestamp() : getCurrentDateTime(config);

        Optional<WorkerEntity> workerOpt = workerRepository.findByFingerprintId(request.getFingerprintId());

        AccessLogEntity accessLog;

        if (workerOpt.isEmpty()) {
            // Huella no reconocida
            log.warn("Fingerprint not recognized: {}", request.getFingerprintId());

            accessLog = AccessLogEntity.builder()
                    .fingerprintId(request.getFingerprintId())
                    .status(AccessStatus.FINGERPRINT_NOT_RECOGNIZED)
                    .location(request.getLocation())
                    .denialReason("Fingerprint not registered in system")
                    .accessTime(accessTime)
                    .build();

            accessLogRepository.save(accessLog);

            // Registrar intento fallido en seguridad
            securityService.logFailedAccessAttempt(request.getFingerprintId());

            // Sincronizar con Firebase
            firebaseService.logAccessDenied(request.getFingerprintId(), "Fingerprint not recognized");

            return mapToResponse(accessLog, null);
        }

        WorkerEntity worker = workerOpt.get();

        // Verificar si el trabajador tiene acceso al área restringida
        if (!worker.isHasRestrictedAreaAccess()) {
            log.warn("Worker {} does not have restricted area access", worker.getId());

            accessLog = AccessLogEntity.builder()
                    .workerId(worker.getId())
                    .fingerprintId(request.getFingerprintId())
                    .status(AccessStatus.UNAUTHORIZED)
                    .location(request.getLocation())
                    .denialReason("Worker does not have restricted area access permissions")
                    .accessTime(accessTime)
                    .build();

            accessLogRepository.save(accessLog);

            // Sincronizar con Firebase
            firebaseService.logAccessDenied(request.getFingerprintId(), "No permissions");

            return mapToResponse(accessLog, worker);
        }

        // Acceso concedido
        log.info("Access granted to worker {} (ID: {})", worker.getFirstName(), worker.getId());

        accessLog = AccessLogEntity.builder()
                .workerId(worker.getId())
                .fingerprintId(request.getFingerprintId())
                .status(AccessStatus.GRANTED)
                .location(request.getLocation())
                .accessTime(accessTime)
                .build();

        accessLogRepository.save(accessLog);

        // Sincronizar con Firebase
        firebaseService.logAccessGranted(request.getFingerprintId(), worker.getId());

        return mapToResponse(accessLog, worker);
    }

    @Override
    @Transactional(readOnly = true)
    public AccessLogResponse getAccessLogById(Long id) {
        AccessLogEntity entity = accessLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Access log not found with ID: " + id
                ));

        WorkerEntity worker = null;
        if (entity.getWorkerId() != null) {
            worker = workerRepository.findById(entity.getWorkerId()).orElse(null);
        }

        return mapToResponse(entity, worker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getAccessLogsByWorker(Long workerId) {
        List<AccessLogEntity> entities = accessLogRepository.findByWorkerIdOrderByAccessTimeDesc(workerId);
        return mapToResponseList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getAccessLogsByTimeRange(
            LocalDateTime startTime, LocalDateTime endTime) {

        List<AccessLogEntity> entities = accessLogRepository.findByAccessTimeBetween(startTime, endTime);
        return mapToResponseList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getRecentDeniedAccess(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<AccessLogEntity> entities = accessLogRepository.findRecentDeniedAccess(since);
        return mapToResponseList(entities);
    }

    private SystemConfigurationEntity getCurrentConfiguration() {
        return configRepository.findLatestConfiguration()
                .orElseGet(() -> {
                    log.warn("No system configuration found, using defaults");
                    return SystemConfigurationEntity.builder()
                            .simulationMode(false)
                            .build();
                });
    }

    private LocalDateTime getCurrentDateTime(SystemConfigurationEntity config) {
        if (config.isSimulationMode() && config.getSimulatedDateTime() != null) {
            return config.getSimulatedDateTime();
        }
        return LocalDateTime.now();
    }

    private AccessLogResponse mapToResponse(AccessLogEntity entity, WorkerEntity worker) {
        return AccessLogResponse.builder()
                .id(entity.getId())
                .workerId(entity.getWorkerId())
                .workerFullName(worker != null ? worker.getFirstName() + " " + worker.getLastName() : null)
                .fingerprintId(entity.getFingerprintId())
                .status(entity.getStatus())
                .location(entity.getLocation())
                .denialReason(entity.getDenialReason())
                .accessTime(entity.getAccessTime())
                .build();
    }

    private List<AccessLogResponse> mapToResponseList(List<AccessLogEntity> entities) {
        return entities.stream()
                .map(entity -> {
                    WorkerEntity worker = null;
                    if (entity.getWorkerId() != null) {
                        worker = workerRepository.findById(entity.getWorkerId()).orElse(null);
                    }
                    return mapToResponse(entity, worker);
                })
                .collect(Collectors.toList());
    }
}