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
                .workerSnapshotName(workerOpt.map(w -> w.getFirstName() + " " + w.getLastName()).orElse("Desconocido"))
                .fingerprintId(fingerprintId)
                .accessGranted(true)
                .location(null)
                .status("GRANTED")
                .accessTime(timestamp)
                .build();

        accessLogRepository.save(entity);
    }

    @Override
    public void logAccessDenied(Integer fingerprintId, LocalDateTime timestamp) {
        log.info("Logging access DENIED for fingerprint ID: {}", fingerprintId);

        Long workerId = null;
        String snapshotName = "No Registrado / Desconocido";

        if (fingerprintId != null) {
            Optional<WorkerEntity> workerOpt = workerRepository.findByFingerprintId(fingerprintId);
            if (workerOpt.isPresent()) {
                WorkerEntity w = workerOpt.get();
                workerId = w.getId();
                snapshotName = w.getFirstName() + " " + w.getLastName();
            }
        }

        AccessLogEntity entity = AccessLogEntity.builder()
                .workerId(workerId)
                .workerSnapshotName(snapshotName)
                .fingerprintId(fingerprintId)
                .accessGranted(false)
                .location(null)
                .status("DENIED")
                .accessTime(timestamp)
                .build();

        accessLogRepository.save(entity);
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
    public List<AccessLogResponse> getAccessHistoryByWorker(Long workerId) {
        return mapToResponseList(accessLogRepository.findByWorkerIdOrderByAccessTimeDesc(workerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getRecentDeniedAccess(int hours) {
        return mapToResponseList(accessLogRepository.findRecentDeniedAccess(LocalDateTime.now().minusHours(hours)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getRecentGrantedAccess(int hours) {
        return mapToResponseList(accessLogRepository.findRecentGrantedAccess(LocalDateTime.now().minusHours(hours)));
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
        String workerName = entity.getWorkerSnapshotName();
        if (workerName == null) workerName = "Desconocido";

        if (entity.getWorkerId() != null) {
            Optional<WorkerEntity> w = workerRepository.findById(entity.getWorkerId());
            if (w.isPresent()) {
                workerName = w.get().getFirstName() + " " + w.get().getLastName();
            }
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