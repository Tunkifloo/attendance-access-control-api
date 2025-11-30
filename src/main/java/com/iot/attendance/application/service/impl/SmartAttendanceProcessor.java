package com.iot.attendance.application.service.impl;

import com.iot.attendance.application.dto.request.RfidAttendanceRequest;
import com.iot.attendance.application.dto.response.AttendanceResponse;
import com.iot.attendance.application.service.AttendanceService;
import com.iot.attendance.infrastructure.exception.ResourceNotFoundException;
import com.iot.attendance.infrastructure.persistence.entity.AttendanceEntity;
import com.iot.attendance.infrastructure.persistence.entity.RfidCardEntity;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import com.iot.attendance.infrastructure.persistence.repository.AttendanceRepository;
import com.iot.attendance.infrastructure.persistence.repository.RfidCardRepository;
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
    private final RfidCardRepository rfidCardRepository;

    public AttendanceResponse processRfidEvent(RfidAttendanceRequest request) {
        log.info("Processing smart RFID event: {}", request.getRfidUid());

        // Normalizar
        String normalizedRfid = request.getRfidUid().toUpperCase().replace(" ", "").trim();

        // Buscar Tarjeta
        RfidCardEntity card = rfidCardRepository.findById(normalizedRfid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RFID Tag not registered in system: " + normalizedRfid
                ));

        // Obtener Worker
        WorkerEntity worker = card.getWorker();
        if (worker == null) {
            throw new ResourceNotFoundException("RFID Tag exists but is not assigned to any worker");
        }

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