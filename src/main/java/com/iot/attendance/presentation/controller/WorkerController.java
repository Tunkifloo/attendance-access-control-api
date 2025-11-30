package com.iot.attendance.presentation.controller;

import com.iot.attendance.application.dto.request.AddRfidTagRequest;
import com.iot.attendance.application.dto.request.AssignFingerprintRequest;
import com.iot.attendance.application.dto.request.CreateWorkerRequest;
import com.iot.attendance.application.dto.request.UpdateWorkerRequest;
import com.iot.attendance.application.dto.response.ApiResponse;
import com.iot.attendance.application.dto.response.WorkerResponse;
import com.iot.attendance.application.service.WorkerService;
import com.iot.attendance.domain.enums.WorkerStatus;
import com.iot.attendance.infrastructure.persistence.entity.RfidCardEntity;
import com.iot.attendance.infrastructure.persistence.repository.RfidCardRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workers", description = "Gestión de trabajadores")
public class WorkerController {

    private final WorkerService workerService;
    private final RfidCardRepository rfidCardRepository;


    @GetMapping("/rfid/unassigned")
    @Operation(summary = "Listar tarjetas RFID disponibles",
            description = "Devuelve los UIDs de las tarjetas que han sido escaneadas pero no tienen dueño asignado")
    public ResponseEntity<ApiResponse<List<String>>> getUnassignedRfidCards() {
        List<String> unassignedCards = rfidCardRepository.findByWorkerIsNull().stream()
                .map(RfidCardEntity::getUid)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(unassignedCards));
    }

    @PostMapping("/{id}/rfid-tags")
    @Operation(summary = "Asignar tarjeta RFID a trabajador",
            description = "Vincula una tarjeta existente del pool al trabajador")
    public ResponseEntity<ApiResponse<WorkerResponse>> addRfidTag(
            @PathVariable Long id,
            @RequestBody AddRfidTagRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                "RFID tag linked successfully",
                workerService.addRfidTag(id, request.getRfidTag())
        ));
    }

    @DeleteMapping("/{id}/rfid-tags")
    @Operation(summary = "Desvincular tarjeta RFID",
            description = "Quita la tarjeta del trabajador y la devuelve al pool de disponibles")
    public ResponseEntity<ApiResponse<WorkerResponse>> removeRfidTag(
            @PathVariable Long id,
            @RequestParam String rfidTag) {

        log.info("Removing RFID tag {} from worker {}", rfidTag, id);
        WorkerResponse response = workerService.removeRfidTag(id, rfidTag);

        return ResponseEntity.ok(ApiResponse.success("RFID tag unlinked (returned to pool)", response));
    }

    @PostMapping
    @Operation(summary = "Crear trabajador",
            description = "Inicia el modo registro en el ESP32 y ESPERA hasta obtener la huella. " +
                    "Esta operación puede tardar hasta 40 segundos.")
    public ResponseEntity<ApiResponse<WorkerResponse>> createWorker(
            @Valid @RequestBody CreateWorkerRequest request) {

        log.info("Starting worker creation flow for: {}", request.getDocumentNumber());
        WorkerResponse response = workerService.createWorker(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Worker created and fingerprint registered successfully", response));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Crear trabajadores en lote", description = "Carga una lista de trabajadores (Sin registro biométrico automático)")
    public ResponseEntity<ApiResponse<List<WorkerResponse>>> bulkCreateWorkers(
            @Valid @RequestBody List<CreateWorkerRequest> requests) {

        log.info("Bulk creating {} workers", requests.size());
        List<WorkerResponse> responses = workerService.bulkCreateWorkers(requests);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        String.format("%d workers created successfully", responses.size()),
                        responses
                ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener trabajador por ID")
    public ResponseEntity<ApiResponse<WorkerResponse>> getWorkerById(
            @Parameter(description = "ID del trabajador") @PathVariable Long id) {

        WorkerResponse response = workerService.getWorkerById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/document/{documentNumber}")
    @Operation(summary = "Buscar trabajador por número de documento")
    public ResponseEntity<ApiResponse<WorkerResponse>> getWorkerByDocument(
            @Parameter(description = "Número de documento") @PathVariable String documentNumber) {

        WorkerResponse response = workerService.getWorkerByDocumentNumber(documentNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/fingerprint/{fingerprintId}")
    @Operation(summary = "Buscar trabajador por ID de huella")
    public ResponseEntity<ApiResponse<WorkerResponse>> getWorkerByFingerprint(
            @Parameter(description = "ID de huella dactilar") @PathVariable Integer fingerprintId) {

        WorkerResponse response = workerService.getWorkerByFingerprintId(fingerprintId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/rfid/{rfidTag}")
    @Operation(summary = "Buscar trabajador por tag RFID")
    public ResponseEntity<ApiResponse<WorkerResponse>> getWorkerByRfid(
            @Parameter(description = "UID del tag RFID") @PathVariable String rfidTag) {

        WorkerResponse response = workerService.getWorkerByRfidTag(rfidTag);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Listar todos los trabajadores")
    public ResponseEntity<ApiResponse<List<WorkerResponse>>> getAllWorkers() {
        List<WorkerResponse> responses = workerService.getAllWorkers();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Listar trabajadores por estado")
    public ResponseEntity<ApiResponse<List<WorkerResponse>>> getWorkersByStatus(
            @Parameter(description = "Estado del trabajador") @PathVariable WorkerStatus status) {

        List<WorkerResponse> responses = workerService.getWorkersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/restricted-access")
    @Operation(summary = "Listar trabajadores con acceso a área restringida")
    public ResponseEntity<ApiResponse<List<WorkerResponse>>> getWorkersWithRestrictedAccess() {
        List<WorkerResponse> responses = workerService.getActiveWorkersWithRestrictedAccess();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar información del trabajador")
    public ResponseEntity<ApiResponse<WorkerResponse>> updateWorker(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkerRequest request) {

        log.info("Updating worker: {}", id);
        WorkerResponse response = workerService.updateWorker(id, request);

        return ResponseEntity.ok(ApiResponse.success("Worker updated successfully", response));
    }

    @PostMapping("/{id}/fingerprint")
    @Operation(summary = "Asignar huella dactilar manualmente (Solo si falló el registro automático)")
    public ResponseEntity<ApiResponse<WorkerResponse>> assignFingerprint(
            @PathVariable Long id,
            @Valid @RequestBody AssignFingerprintRequest request) {

        log.info("Manually assigning fingerprint {} to worker {}", request.getFingerprintId(), id);
        WorkerResponse response = workerService.assignFingerprint(id, request);

        return ResponseEntity.ok(ApiResponse.success("Fingerprint assigned successfully", response));
    }

    @PostMapping("/{id}/grant-access")
    @Operation(summary = "Otorgar acceso a área restringida")
    public ResponseEntity<ApiResponse<WorkerResponse>> grantRestrictedAccess(@PathVariable Long id) {
        log.info("Granting restricted area access to worker {}", id);
        WorkerResponse response = workerService.grantRestrictedAreaAccess(id);

        return ResponseEntity.ok(ApiResponse.success("Access granted successfully", response));
    }

    @PostMapping("/{id}/revoke-access")
    @Operation(summary = "Revocar acceso a área restringida")
    public ResponseEntity<ApiResponse<WorkerResponse>> revokeRestrictedAccess(@PathVariable Long id) {
        log.info("Revoking restricted area access from worker {}", id);
        WorkerResponse response = workerService.revokeRestrictedAreaAccess(id);

        return ResponseEntity.ok(ApiResponse.success("Access revoked successfully", response));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activar trabajador")
    public ResponseEntity<ApiResponse<WorkerResponse>> activateWorker(@PathVariable Long id) {
        log.info("Activating worker {}", id);
        WorkerResponse response = workerService.activateWorker(id);

        return ResponseEntity.ok(ApiResponse.success("Worker activated successfully", response));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar trabajador")
    public ResponseEntity<ApiResponse<WorkerResponse>> deactivateWorker(@PathVariable Long id) {
        log.info("Deactivating worker {}", id);
        WorkerResponse response = workerService.deactivateWorker(id);

        return ResponseEntity.ok(ApiResponse.success("Worker deactivated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar trabajador")
    public ResponseEntity<ApiResponse<Void>> deleteWorker(@PathVariable Long id) {
        log.info("Deleting worker {}", id);
        workerService.deleteWorker(id);

        return ResponseEntity.ok(ApiResponse.success("Worker deleted successfully", null));
    }
}