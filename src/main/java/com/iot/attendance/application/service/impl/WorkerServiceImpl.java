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
import com.iot.attendance.infrastructure.persistence.entity.RfidCardEntity;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import com.iot.attendance.infrastructure.persistence.repository.RfidCardRepository;
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
    private final RfidCardRepository rfidCardRepository;
    private final WorkerMapper workerMapper;
    private final FirebaseRealtimeService firebaseService;

    @Override
    public WorkerResponse createWorker(CreateWorkerRequest request) {
        log.info("Iniciando creación de trabajador con Registro Biométrico Síncrono...");

        if (workerRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new ResourceAlreadyExistsException("Documento ya existe: " + request.getDocumentNumber());
        }

        // Activar Hardware y ESPERAR huella
        firebaseService.startRegistrationMode();

        // Bloqueamos aquí hasta 40 segundos esperando al usuario
        Integer newFingerprintId = firebaseService.waitForNewFingerprintId(40);
        log.info("¡Huella capturada exitosamente! ID: {}", newFingerprintId);

        if (workerRepository.existsByFingerprintId(newFingerprintId)) {
            firebaseService.setAdminCommand("NADA");
            throw new ResourceAlreadyExistsException("La huella ID " + newFingerprintId + " ya pertenece a otro trabajador.");
        }
        // Guardar Worker
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

        // Resetear Hardware
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

        // No actualizamos tarjetas aquí, eso se hace via addRfidTag/removeRfidTag

        entity.setUpdatedAt(LocalDateTime.now());
        WorkerEntity updated = workerRepository.save(entity);

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
        // Buscamos primero la tarjeta y luego obtenemos el worker
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
        // Este método podría ser redundante si usas el createWorker nuevo,
        // pero lo mantenemos por si quieres actualizar huella manualmente.
        log.info("Assigning fingerprint {} to worker {}", request.getFingerprintId(), workerId);

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
        log.info("Assigning RFID tag {} to worker {}", normalizedTag, workerId);

        WorkerEntity worker = findWorkerEntityById(workerId);

        // Buscar la tarjeta en el pool (o crearla si no existe, aunque lo ideal es que venga del pool)
        RfidCardEntity card = rfidCardRepository.findById(normalizedTag)
                .orElseGet(() -> RfidCardEntity.builder()
                        .uid(normalizedTag)
                        .lastSeen(LocalDateTime.now())
                        .build());

        // Validar si ya pertenece a otro
        if (card.getWorker() != null && !card.getWorker().getId().equals(workerId)) {
            throw new ResourceAlreadyExistsException("RFID tag is already assigned to worker: " + card.getWorker().getId());
        }

        // Asignar
        card.setWorker(worker);
        card.setUpdatedAt(LocalDateTime.now());

        // Guardamos la tarjeta (la relación se actualiza por el lado propietario)
        rfidCardRepository.save(card);

        // Agregamos manualmente la tarjeta a la lista del worker para que el Mapper la vea
        worker.getRfidCards().add(card);

        // Refrescamos el worker para la respuesta
        return mapToResponse(worker);
    }

    @Override
    public WorkerResponse removeRfidTag(Long workerId, String rfidTag) {
        String normalizedTag = rfidTag.toUpperCase().replace(" ", "").trim();
        log.info("Removing RFID tag {} from worker {}", normalizedTag, workerId);

        RfidCardEntity card = rfidCardRepository.findById(normalizedTag)
                .orElseThrow(() -> new ResourceNotFoundException("RFID Tag not found"));

        if (card.getWorker() == null || !card.getWorker().getId().equals(workerId)) {
            throw new ResourceNotFoundException("RFID Tag is not assigned to this worker");
        }

        // Desvincular (Volverla huérfana al pool)
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
    public void deleteWorker(Long workerId) {
        WorkerEntity entity = findWorkerEntityById(workerId);
        workerRepository.delete(entity);
    }

    @Override
    public List<WorkerResponse> bulkCreateWorkers(List<CreateWorkerRequest> requests) {
        // Implementación simple sin bloqueo biométrico
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
        // Mapeo manual porque MapStruct puede fallar con la nueva estructura
        // si no se actualizó el Mapper.
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