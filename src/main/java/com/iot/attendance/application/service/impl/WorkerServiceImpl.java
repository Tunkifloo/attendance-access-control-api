package com.iot.attendance.application.service.impl;

import com.iot.attendance.application.dto.request.AssignFingerprintRequest;
import com.iot.attendance.application.dto.request.CreateWorkerRequest;
import com.iot.attendance.application.dto.request.UpdateWorkerRequest;
import com.iot.attendance.application.dto.response.WorkerResponse;
import com.iot.attendance.application.mapper.WorkerMapper;
import com.iot.attendance.application.service.WorkerService;
import com.iot.attendance.domain.enums.WorkerStatus;
import com.iot.attendance.domain.model.Worker;
import com.iot.attendance.domain.valueobjects.FingerprintId;
import com.iot.attendance.domain.valueobjects.RfidTag;
import com.iot.attendance.infrastructure.exception.ResourceAlreadyExistsException;
import com.iot.attendance.infrastructure.exception.ResourceNotFoundException;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import com.iot.attendance.infrastructure.persistence.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkerServiceImpl implements WorkerService {

    private final WorkerRepository workerRepository;
    private final WorkerMapper workerMapper;

    @Override
    public WorkerResponse createWorker(CreateWorkerRequest request) {
        log.info("Creating worker with document number: {}", request.getDocumentNumber());

        if (workerRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new ResourceAlreadyExistsException(
                    "Worker with document number " + request.getDocumentNumber() + " already exists"
            );
        }

        WorkerEntity entity = WorkerEntity.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .documentNumber(request.getDocumentNumber())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .rfidTags(request.getRfidTags() != null ? request.getRfidTags() : new HashSet<>())
                .hasRestrictedAreaAccess(request.isHasRestrictedAreaAccess())
                .status(WorkerStatus.ACTIVE)
                .build();

        WorkerEntity saved = workerRepository.save(entity);
        log.info("Worker created successfully with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public WorkerResponse updateWorker(Long id, UpdateWorkerRequest request) {
        log.info("Updating worker with ID: {}", id);

        WorkerEntity entity = findWorkerEntityById(id);

        if (request.getFirstName() != null) {
            entity.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            entity.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            entity.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            entity.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getHasRestrictedAreaAccess() != null) {
            entity.setHasRestrictedAreaAccess(request.getHasRestrictedAreaAccess());
        }

        entity.setUpdatedAt(LocalDateTime.now());
        WorkerEntity updated = workerRepository.save(entity);

        log.info("Worker updated successfully: {}", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponse getWorkerById(Long id) {
        WorkerEntity entity = findWorkerEntityById(id);
        return mapToResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponse getWorkerByDocumentNumber(String documentNumber) {
        WorkerEntity entity = workerRepository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with document number: " + documentNumber
                ));
        return mapToResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponse getWorkerByFingerprintId(Integer fingerprintId) {
        WorkerEntity entity = workerRepository.findByFingerprintId(fingerprintId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with fingerprint ID: " + fingerprintId
                ));
        return mapToResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponse getWorkerByRfidTag(String rfidTag) {
        WorkerEntity entity = workerRepository.findByRfidTag(rfidTag.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with RFID tag: " + rfidTag
                ));
        return mapToResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerResponse> getAllWorkers() {
        return workerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerResponse> getWorkersByStatus(WorkerStatus status) {
        return workerRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerResponse> getActiveWorkersWithRestrictedAccess() {
        return workerRepository.findActiveWorkersWithRestrictedAccess().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WorkerResponse assignFingerprint(Long workerId, AssignFingerprintRequest request) {
        log.info("Assigning fingerprint {} to worker {}", request.getFingerprintId(), workerId);

        if (workerRepository.existsByFingerprintId(request.getFingerprintId())) {
            throw new ResourceAlreadyExistsException(
                    "Fingerprint ID " + request.getFingerprintId() + " is already assigned"
            );
        }

        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.setFingerprintId(request.getFingerprintId());
        entity.setUpdatedAt(LocalDateTime.now());

        WorkerEntity updated = workerRepository.save(entity);
        log.info("Fingerprint assigned successfully");

        return mapToResponse(updated);
    }

    @Override
    public WorkerResponse addRfidTag(Long workerId, String rfidTag) {
        log.info("Adding RFID tag {} to worker {}", rfidTag, workerId);

        String normalizedTag = rfidTag.toUpperCase().trim();

        if (workerRepository.existsByRfidTag(normalizedTag)) {
            throw new ResourceAlreadyExistsException(
                    "RFID tag " + normalizedTag + " is already assigned"
            );
        }

        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.getRfidTags().add(normalizedTag);
        entity.setUpdatedAt(LocalDateTime.now());

        WorkerEntity updated = workerRepository.save(entity);
        log.info("RFID tag added successfully");

        return mapToResponse(updated);
    }

    @Override
    public WorkerResponse removeRfidTag(Long workerId, String rfidTag) {
        log.info("Removing RFID tag {} from worker {}", rfidTag, workerId);

        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.getRfidTags().remove(rfidTag.toUpperCase().trim());
        entity.setUpdatedAt(LocalDateTime.now());

        WorkerEntity updated = workerRepository.save(entity);
        log.info("RFID tag removed successfully");

        return mapToResponse(updated);
    }

    @Override
    public WorkerResponse grantRestrictedAreaAccess(Long workerId) {
        log.info("Granting restricted area access to worker {}", workerId);

        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.setHasRestrictedAreaAccess(true);
        entity.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(workerRepository.save(entity));
    }

    @Override
    public WorkerResponse revokeRestrictedAreaAccess(Long workerId) {
        log.info("Revoking restricted area access from worker {}", workerId);

        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.setHasRestrictedAreaAccess(false);
        entity.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(workerRepository.save(entity));
    }

    @Override
    public WorkerResponse activateWorker(Long workerId) {
        log.info("Activating worker {}", workerId);

        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.setStatus(WorkerStatus.ACTIVE);
        entity.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(workerRepository.save(entity));
    }

    @Override
    public WorkerResponse deactivateWorker(Long workerId) {
        log.info("Deactivating worker {}", workerId);

        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.setStatus(WorkerStatus.INACTIVE);
        entity.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(workerRepository.save(entity));
    }

    @Override
    public void deleteWorker(Long workerId) {
        log.info("Deleting worker {}", workerId);

        WorkerEntity entity = findWorkerEntityById(workerId);
        workerRepository.delete(entity);

        log.info("Worker deleted successfully");
    }

    @Override
    public List<WorkerResponse> bulkCreateWorkers(List<CreateWorkerRequest> requests) {
        log.info("Bulk creating {} workers", requests.size());

        List<WorkerEntity> entities = requests.stream()
                .map(req -> WorkerEntity.builder()
                        .firstName(req.getFirstName())
                        .lastName(req.getLastName())
                        .documentNumber(req.getDocumentNumber())
                        .email(req.getEmail())
                        .phoneNumber(req.getPhoneNumber())
                        .rfidTags(req.getRfidTags() != null ? req.getRfidTags() : new HashSet<>())
                        .hasRestrictedAreaAccess(req.isHasRestrictedAreaAccess())
                        .status(WorkerStatus.ACTIVE)
                        .build())
                .collect(Collectors.toList());

        List<WorkerEntity> saved = workerRepository.saveAll(entities);
        log.info("Bulk creation completed: {} workers created", saved.size());

        return saved.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private WorkerEntity findWorkerEntityById(Long id) {
        return workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with ID: " + id));
    }

    private WorkerResponse mapToResponse(WorkerEntity entity) {
        Worker domain = workerMapper.toDomain(entity);

        return WorkerResponse.builder()
                .id(domain.getId())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .fullName(domain.getFullName())
                .documentNumber(domain.getDocumentNumber())
                .email(domain.getEmail())
                .phoneNumber(domain.getPhoneNumber())
                .fingerprintId(domain.getFingerprintId() != null ? domain.getFingerprintId().getValue() : null)
                .rfidTags(domain.getRfidTags().stream()
                        .map(RfidTag::getUid)
                        .collect(Collectors.toSet()))
                .hasRestrictedAreaAccess(domain.isHasRestrictedAreaAccess())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
