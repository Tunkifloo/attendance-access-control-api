package com.iot.attendance.application.service.impl;

import com.iot.attendance.application.dto.response.AccessLogResponse;
import com.iot.attendance.application.mapper.AccessLogMapper;
import com.iot.attendance.application.service.AccessAuditService;
import com.iot.attendance.infrastructure.persistence.entity.AccessLogEntity;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import com.iot.attendance.infrastructure.persistence.repository.AccessLogRepository;
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
public class AccessAuditServiceImpl implements AccessAuditService {

    private final AccessLogRepository accessLogRepository;
    private final WorkerRepository workerRepository;
    @Getter
    private final AccessLogMapper accessLogMapper;

    @Override
    public void logAccessGranted(Integer fingerprintId, LocalDateTime timestamp) {
        log.info("Logging access GRANTED for fingerprint ID: {}", fingerprintId);

        Optional<WorkerEntity> workerOpt = workerRepository.findByFingerprintId(fingerprintId);

        AccessLogEntity entity = AccessLogEntity.builder()
                .workerId(workerOpt.map(WorkerEntity::getId).orElse(null))
                .fingerprintId(fingerprintId)
                .accessGranted(true)
                .location("Área Restringida")
                .accessTime(timestamp)
                .build();

        accessLogRepository.save(entity);

        if (workerOpt.isPresent()) {
            log.info("✓ Access granted logged for worker: {} ({})",
                    workerOpt.get().getFirstName(),
                    workerOpt.get().getDocumentNumber());
        } else {
            log.warn("⚠ Access granted logged but fingerprint {} not linked to any worker",
                    fingerprintId);
        }
    }

    @Override
    public void logAccessDenied(Integer fingerprintId, LocalDateTime timestamp) {
        log.info("Logging access DENIED for fingerprint ID: {}", fingerprintId);

        AccessLogEntity entity = AccessLogEntity.builder()
                .workerId(null)
                .fingerprintId(fingerprintId)
                .accessGranted(false)
                .location("Área Restringida")
                .accessTime(timestamp)
                .build();

        accessLogRepository.save(entity);
        log.info("✓ Access denied logged");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getAccessHistoryByWorker(Long workerId) {
        log.info("Fetching access history for worker: {}", workerId);

        List<AccessLogEntity> entities = accessLogRepository
                .findByWorkerIdOrderByAccessTimeDesc(workerId);

        return mapToResponseList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getAccessHistoryByTimeRange(
            LocalDateTime startTime, LocalDateTime endTime) {

        log.info("Fetching access history from {} to {}", startTime, endTime);

        List<AccessLogEntity> entities = accessLogRepository
                .findByAccessTimeBetween(startTime, endTime);

        return mapToResponseList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getRecentDeniedAccess(int hours) {
        log.info("Fetching denied accesses from last {} hours", hours);

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<AccessLogEntity> entities = accessLogRepository
                .findRecentDeniedAccess(since);

        return mapToResponseList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getRecentGrantedAccess(int hours) {
        log.info("Fetching granted accesses from last {} hours", hours);

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<AccessLogEntity> entities = accessLogRepository
                .findRecentGrantedAccess(since);

        return mapToResponseList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDeniedAccessesByWorker(Long workerId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("Counting denied accesses for worker {} from {} to {}",
                workerId, startTime, endTime);

        return accessLogRepository.countDeniedAccessesByWorker(workerId, startTime, endTime);
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

    private AccessLogResponse mapToResponse(AccessLogEntity entity, WorkerEntity worker) {
        String workerName = (worker != null)
                ? worker.getFirstName() + " " + worker.getLastName()
                : "Desconocido";

        return AccessLogResponse.builder()
                .id(entity.getId())
                .workerId(entity.getWorkerId())
                .workerFullName(workerName)
                .fingerprintId(entity.getFingerprintId())
                .accessGranted(entity.isAccessGranted())
                .location(entity.getLocation())
                .accessTime(entity.getAccessTime())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}