package com.iot.attendance.application.service;

import com.iot.attendance.application.dto.request.AssignFingerprintRequest;
import com.iot.attendance.application.dto.request.CreateWorkerRequest;
import com.iot.attendance.application.dto.request.UpdateWorkerRequest;
import com.iot.attendance.application.dto.response.WorkerResponse;
import com.iot.attendance.domain.enums.WorkerStatus;

import java.util.List;

public interface WorkerService {

    WorkerResponse createWorker(CreateWorkerRequest request);

    WorkerResponse updateWorker(Long id, UpdateWorkerRequest request);

    WorkerResponse getWorkerById(Long id);

    WorkerResponse getWorkerByDocumentNumber(String documentNumber);

    WorkerResponse getWorkerByFingerprintId(Integer fingerprintId);

    WorkerResponse getWorkerByRfidTag(String rfidTag);

    List<WorkerResponse> getAllWorkers();

    List<WorkerResponse> getWorkersByStatus(WorkerStatus status);

    List<WorkerResponse> getActiveWorkersWithRestrictedAccess();

    WorkerResponse assignFingerprint(Long workerId, AssignFingerprintRequest request);

    WorkerResponse addRfidTag(Long workerId, String rfidTag);

    WorkerResponse removeRfidTag(Long workerId, String rfidTag);

    WorkerResponse grantRestrictedAreaAccess(Long workerId);

    WorkerResponse revokeRestrictedAreaAccess(Long workerId);

    WorkerResponse activateWorker(Long workerId);

    WorkerResponse deactivateWorker(Long workerId);

    void deleteWorker(Long workerId);

    List<WorkerResponse> bulkCreateWorkers(List<CreateWorkerRequest> requests);
}
