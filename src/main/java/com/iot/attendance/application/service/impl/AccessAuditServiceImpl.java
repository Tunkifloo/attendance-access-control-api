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
import org.springframework.data.domain.Sort;
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
                .workerSnapshotName(workerOpt.map(w -> w.getFirstName() + " " + w.getLastName()).orElse(null))
                .fingerprintId(fingerprintId)
                .accessGranted(true)
                .location("Puerta Principal")
                .status("GRANTED")
                .accessTime(timestamp)
                .build();

        accessLogRepository.save(entity);
    }

    @Override
    public void logAccessDenied(Integer fingerprintId, LocalDateTime timestamp) {
        log.info("Logging access DENIED for fingerprint ID: {}", fingerprintId);
        String workerName = null;
        if (fingerprintId != null) {
            workerName = workerRepository.findByFingerprintId(fingerprintId)
                    .map(w -> w.getFirstName() + " " + w.getLastName())
                    .orElse(null);
        }

        AccessLogEntity entity = AccessLogEntity.builder()
                .workerId(null)
                .workerSnapshotName(workerName)
                .fingerprintId(fingerprintId)
                .accessGranted(false)
                .location("Puerta Principal")
                .status("DENIED")
                .accessTime(timestamp)
                .build();

        accessLogRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getAccessHistoryByWorker(Long workerId) {
        return mapToResponseList(accessLogRepository.findByWorkerIdOrderByAccessTimeDesc(workerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getAccessHistoryByTimeRange(
            LocalDateTime startTime, LocalDateTime endTime, String status, String sortDirection) {

        Sort sort = Sort.by("accessTime");
        if ("ASC".equalsIgnoreCase(sortDirection)) sort = sort.ascending();
        else sort = sort.descending();

        List<AccessLogEntity> entities;
        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            entities = accessLogRepository.findByAccessTimeBetweenAndStatus(startTime, endTime, status, sort);
        } else {
            entities = accessLogRepository.findByAccessTimeBetween(startTime, endTime, sort);
        }

        return mapToResponseList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getRecentDeniedAccess(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return mapToResponseList(accessLogRepository.findRecentDeniedAccess(since));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getRecentGrantedAccess(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return mapToResponseList(accessLogRepository.findRecentGrantedAccess(since));
    }

    @Override
    @Transactional(readOnly = true)
    public long countDeniedAccessesByWorker(Long workerId, LocalDateTime startTime, LocalDateTime endTime) {
        return accessLogRepository.countDeniedAccessesByWorker(workerId, startTime, endTime);
    }

    private List<AccessLogResponse> mapToResponseList(List<AccessLogEntity> entities) {
        return entities.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private AccessLogResponse mapToResponse(AccessLogEntity entity) {
        String workerName = "Desconocido";

        if (entity.getWorkerId() != null) {
            Optional<WorkerEntity> w = workerRepository.findById(entity.getWorkerId());
            if (w.isPresent()) {
                workerName = w.get().getFirstName() + " " + w.get().getLastName();
            } else if (entity.getWorkerSnapshotName() != null) {
                // Si tiene ID pero no está en DB (inconsistencia rara), usar snapshot
                workerName = entity.getWorkerSnapshotName() + " (Eliminado)";
            }
        }
        else if (entity.getWorkerSnapshotName() != null) {
            workerName = entity.getWorkerSnapshotName() + " (Eliminado)";
        }

        return AccessLogResponse.builder()
                .id(entity.getId())
                .workerId(entity.getWorkerId())
                .workerFullName(workerName)
                .fingerprintId(entity.getFingerprintId())
                .accessGranted(entity.isAccessGranted())
                .location(entity.getLocation())
                .status(entity.getStatus())
                .accessTime(entity.getAccessTime())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}