package com.iot.attendance.application.service.impl;

import com.iot.attendance.application.dto.request.AssignFingerprintRequest;
import com.iot.attendance.application.dto.request.CreateWorkerRequest;
import com.iot.attendance.application.dto.request.UpdateWorkerRequest;
import com.iot.attendance.application.dto.response.WorkerResponse;
import com.iot.attendance.application.mapper.WorkerMapper;
import com.iot.attendance.application.service.WorkerService;
import com.iot.attendance.domain.enums.WorkerStatus;
import com.iot.attendance.infrastructure.exception.ResourceAlreadyExistsException;
import com.iot.attendance.infrastructure.exception.ResourceNotFoundException;
import com.iot.attendance.infrastructure.firebase.FirebaseRealtimeService;
import com.iot.attendance.infrastructure.persistence.entity.AccessLogEntity;
import com.iot.attendance.infrastructure.persistence.entity.AttendanceEntity;
import com.iot.attendance.infrastructure.persistence.entity.RfidCardEntity;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import com.iot.attendance.infrastructure.persistence.repository.AccessLogRepository;
import com.iot.attendance.infrastructure.persistence.repository.AttendanceRepository;
import com.iot.attendance.infrastructure.persistence.repository.RfidCardRepository;
import com.iot.attendance.infrastructure.persistence.repository.WorkerRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkerServiceImpl implements WorkerService {

    private final WorkerRepository workerRepository;
    private final RfidCardRepository rfidCardRepository;
    private final AttendanceRepository attendanceRepository;
    private final AccessLogRepository accessLogRepository;

    @Getter
    private final WorkerMapper workerMapper;
    private final FirebaseRealtimeService firebaseService;

    @Override
    public WorkerResponse createWorker(CreateWorkerRequest request) {
        log.info("Iniciando creación de trabajador con Registro Biométrico Síncrono...");

        if (workerRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new ResourceAlreadyExistsException("Documento ya existe: " + request.getDocumentNumber());
        }

        // 1. Activar Hardware y ESPERAR huella (Bloqueante 40s)
        firebaseService.startRegistrationMode();
        Integer newFingerprintId = firebaseService.waitForNewFingerprintId(40);

        log.info("¡Huella capturada exitosamente! ID: {}", newFingerprintId);

        if (workerRepository.existsByFingerprintId(newFingerprintId)) {
            firebaseService.setAdminCommand("NADA");
            throw new ResourceAlreadyExistsException("La huella ID " + newFingerprintId + " ya pertenece a otro trabajador.");
        }

        // 2. Guardar Worker
        WorkerEntity entity = WorkerEntity.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .documentNumber(request.getDocumentNumber())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .fingerprintId(newFingerprintId)
                .hasRestrictedAreaAccess(request.isHasRestrictedAreaAccess())
                .status(WorkerStatus.ACTIVE)
                .rfidCards(new HashSet<>())
                .build();

        WorkerEntity saved = workerRepository.save(entity);

        // 3. Resetear Hardware
        firebaseService.setAdminCommand("NADA");
        firebaseService.setAdminState("LISTO");

        return mapToResponse(saved);
    }

    @Override
    public WorkerResponse updateWorker(Long id, UpdateWorkerRequest request) {
        log.info("Updating worker with ID: {}", id);
        WorkerEntity entity = findWorkerEntityById(id);

        if (request.getFirstName() != null) entity.setFirstName(request.getFirstName());
        if (request.getLastName() != null) entity.setLastName(request.getLastName());
        if (request.getEmail() != null) entity.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null) entity.setPhoneNumber(request.getPhoneNumber());
        if (request.getHasRestrictedAreaAccess() != null) entity.setHasRestrictedAreaAccess(request.getHasRestrictedAreaAccess());

        entity.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(workerRepository.save(entity));
    }

    @Override
    public void deleteWorker(Long workerId) {
        WorkerEntity worker = findWorkerEntityById(workerId);
        String fullName = worker.getFirstName() + " " + worker.getLastName();

        log.info("Iniciando proceso de borrado seguro para Worker ID: {}", workerId);

        // 1. DESVINCULAR Y GUARDAR SNAPSHOT EN HISTORIAL (Preservar datos)

        // A. Actualizar AccessLogs
        List<AccessLogEntity> accessLogs = accessLogRepository.findByWorkerIdOrderByAccessTimeDesc(workerId);
        for (AccessLogEntity logEntity : accessLogs) {
            logEntity.setWorkerId(null); // Desvincular para evitar borrado en cascada (si hubiera) o referencia rota
            if (logEntity.getWorkerSnapshotName() == null) {
                logEntity.setWorkerSnapshotName(fullName); // Guardar nombre histórico
            }
        }
        accessLogRepository.saveAll(accessLogs);
        log.info("✓ {} logs de acceso desvinculados y preservados.", accessLogs.size());

        // B. Actualizar Attendances
        List<AttendanceEntity> attendances = attendanceRepository.findLatestByWorkerIdOrderByCreatedAtDesc(workerId);
        for (AttendanceEntity att : attendances) {
            att.setWorkerId(null); // Desvincular
            if (att.getWorkerSnapshotName() == null) {
                att.setWorkerSnapshotName(fullName); // Guardar nombre histórico
            }
        }
        attendanceRepository.saveAll(attendances);
        log.info("✓ {} registros de asistencia desvinculados y preservados.", attendances.size());

        // 2. BORRAR HUELLA DEL HARDWARE
        if (worker.getFingerprintId() != null) {
            log.info(">> Enviando comando BORRAR huella ID {} al hardware", worker.getFingerprintId());
            firebaseService.setTargetFingerprintId(worker.getFingerprintId());
            firebaseService.setAdminCommand("BORRAR");
            firebaseService.setAdminState("BORRANDO_USUARIO");

            try {
                firebaseService.waitForDeletionComplete(10);
            } catch (Exception e) {
                log.error("Error esperando confirmación hardware: {}", e.getMessage());
                // Continuamos, la prioridad es limpiar la BD
            }
        }

        // 3. LIBERAR TARJETAS RFID (Devolver al pool)
        if (worker.getRfidCards() != null && !worker.getRfidCards().isEmpty()) {
            List<RfidCardEntity> cardsToRelease = new ArrayList<>(worker.getRfidCards());
            for (RfidCardEntity card : cardsToRelease) {
                log.info(">> Devolviendo tarjeta {} al pool", card.getUid());
                card.setWorker(null);
                card.setUpdatedAt(LocalDateTime.now());
                rfidCardRepository.save(card);
            }
            worker.getRfidCards().clear();
        }

        // 4. ELIMINAR TRABAJADOR DE BASE DE DATOS
        workerRepository.delete(worker);
        log.info("✓ Trabajador ID {} eliminado completamente de la tabla workers.", workerId);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponse getWorkerById(Long id) {
        return mapToResponse(findWorkerEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponse getWorkerByDocumentNumber(String documentNumber) {
        WorkerEntity entity = workerRepository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with document: " + documentNumber));
        return mapToResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponse getWorkerByFingerprintId(Integer fingerprintId) {
        WorkerEntity entity = workerRepository.findByFingerprintId(fingerprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with fingerprint ID: " + fingerprintId));
        return mapToResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponse getWorkerByRfidTag(String rfidTag) {
        String normalizedTag = rfidTag.toUpperCase().replace(" ", "").trim();
        RfidCardEntity card = rfidCardRepository.findById(normalizedTag)
                .orElseThrow(() -> new ResourceNotFoundException("RFID Tag not found: " + normalizedTag));

        if (card.getWorker() == null) {
            throw new ResourceNotFoundException("RFID Tag exists but is not assigned to any worker");
        }
        return mapToResponse(card.getWorker());
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
        if (workerRepository.existsByFingerprintId(request.getFingerprintId())) {
            throw new ResourceAlreadyExistsException("Fingerprint ID already in use");
        }
        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.setFingerprintId(request.getFingerprintId());
        entity.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(workerRepository.save(entity));
    }

    @Override
    public WorkerResponse addRfidTag(Long workerId, String rfidTag) {
        String normalizedTag = rfidTag.toUpperCase().replace(" ", "").trim();
        WorkerEntity worker = findWorkerEntityById(workerId);

        RfidCardEntity card = rfidCardRepository.findById(normalizedTag)
                .orElseGet(() -> RfidCardEntity.builder()
                        .uid(normalizedTag)
                        .lastSeen(LocalDateTime.now())
                        .build());

        if (card.getWorker() != null && !card.getWorker().getId().equals(workerId)) {
            throw new ResourceAlreadyExistsException("RFID tag is already assigned to worker: " + card.getWorker().getId());
        }

        card.setWorker(worker);
        card.setUpdatedAt(LocalDateTime.now());
        rfidCardRepository.save(card);
        worker.getRfidCards().add(card);

        return mapToResponse(worker);
    }

    @Override
    public WorkerResponse removeRfidTag(Long workerId, String rfidTag) {
        String normalizedTag = rfidTag.toUpperCase().replace(" ", "").trim();
        RfidCardEntity card = rfidCardRepository.findById(normalizedTag)
                .orElseThrow(() -> new ResourceNotFoundException("RFID Tag not found"));

        if (card.getWorker() == null || !card.getWorker().getId().equals(workerId)) {
            throw new ResourceNotFoundException("RFID Tag is not assigned to this worker");
        }

        card.setWorker(null);
        card.setUpdatedAt(LocalDateTime.now());
        rfidCardRepository.save(card);

        return mapToResponse(findWorkerEntityById(workerId));
    }

    @Override
    public WorkerResponse grantRestrictedAreaAccess(Long workerId) {
        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.setHasRestrictedAreaAccess(true);
        return mapToResponse(workerRepository.save(entity));
    }

    @Override
    public WorkerResponse revokeRestrictedAreaAccess(Long workerId) {
        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.setHasRestrictedAreaAccess(false);
        return mapToResponse(workerRepository.save(entity));
    }

    @Override
    public WorkerResponse activateWorker(Long workerId) {
        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.setStatus(WorkerStatus.ACTIVE);
        return mapToResponse(workerRepository.save(entity));
    }

    @Override
    public WorkerResponse deactivateWorker(Long workerId) {
        WorkerEntity entity = findWorkerEntityById(workerId);
        entity.setStatus(WorkerStatus.INACTIVE);
        return mapToResponse(workerRepository.save(entity));
    }

    @Override
    public List<WorkerResponse> bulkCreateWorkers(List<CreateWorkerRequest> requests) {
        List<WorkerEntity> entities = requests.stream()
                .map(req -> WorkerEntity.builder()
                        .firstName(req.getFirstName())
                        .lastName(req.getLastName())
                        .documentNumber(req.getDocumentNumber())
                        .email(req.getEmail())
                        .phoneNumber(req.getPhoneNumber())
                        .rfidCards(new HashSet<>())
                        .hasRestrictedAreaAccess(req.isHasRestrictedAreaAccess())
                        .status(WorkerStatus.ACTIVE)
                        .build())
                .collect(Collectors.toList());

        return workerRepository.saveAll(entities).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private WorkerEntity findWorkerEntityById(Long id) {
        return workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with ID: " + id));
    }

    private WorkerResponse mapToResponse(WorkerEntity entity) {
        return WorkerResponse.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .fullName(entity.getFirstName() + " " + entity.getLastName())
                .documentNumber(entity.getDocumentNumber())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .fingerprintId(entity.getFingerprintId())
                .rfidTags(entity.getRfidCards().stream()
                        .map(RfidCardEntity::getUid)
                        .collect(Collectors.toSet()))
                .hasRestrictedAreaAccess(entity.isHasRestrictedAreaAccess())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}