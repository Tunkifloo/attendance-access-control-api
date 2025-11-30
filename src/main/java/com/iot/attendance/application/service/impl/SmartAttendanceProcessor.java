package com.iot.attendance.application.service.impl;

import com.iot.attendance.application.dto.request.RfidAttendanceRequest;
import com.iot.attendance.application.dto.response.AttendanceResponse;
import com.iot.attendance.application.service.AttendanceService;
import com.iot.attendance.infrastructure.exception.ResourceNotFoundException;
import com.iot.attendance.infrastructure.persistence.entity.AttendanceEntity;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import com.iot.attendance.infrastructure.persistence.repository.AttendanceRepository;
import com.iot.attendance.infrastructure.persistence.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SmartAttendanceProcessor {

    private final AttendanceService attendanceService;
    private final AttendanceRepository attendanceRepository;
    private final WorkerRepository workerRepository;

    /**
     * Procesa automáticamente un evento de asistencia RFID
     * Determina inteligentemente si es check-in o check-out
     */
    public AttendanceResponse processRfidEvent(RfidAttendanceRequest request) {
        log.info("Processing smart RFID event: {}", request.getRfidUid());

        // Validar que el trabajador existe
        String normalizedRfid = request.getRfidUid().toUpperCase().trim();
        WorkerEntity worker = workerRepository.findByRfidTag(normalizedRfid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with RFID tag: " + normalizedRfid
                ));

        // Verificar si tiene asistencia activa (check-in sin check-out)
        Optional<AttendanceEntity> activeAttendance =
                attendanceRepository.findActiveAttendanceByWorkerId(worker.getId());

        if (activeAttendance.isPresent()) {
            // Escenario A: Ya tiene check-in -> Hacer CHECK-OUT
            log.info("Worker {} has active check-in. Processing CHECK-OUT", worker.getId());
            return attendanceService.recordCheckOut(request);
        } else {
            // Escenario B: No tiene check-in activo -> Hacer CHECK-IN
            log.info("Worker {} has no active check-in. Processing CHECK-IN", worker.getId());
            return attendanceService.recordCheckIn(request);
        }
    }
}