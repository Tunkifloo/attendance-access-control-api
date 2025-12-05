package com.iot.attendance.application.service.impl;

import com.iot.attendance.application.dto.request.RfidAttendanceRequest;
import com.iot.attendance.application.service.AttendanceService;
import com.iot.attendance.infrastructure.exception.BusinessException;
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
public class SmartAttendanceProcessor {

    private final RfidCardRepository rfidCardRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;

    @Transactional
    public void processRfidEvent(RfidAttendanceRequest request) {
        String uid = request.getRfidUid().toUpperCase().replace(" ", "").trim();
        log.info("Processing smart RFID event: {}", uid);

        RfidCardEntity card = rfidCardRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("RFID not found"));

        WorkerEntity worker = card.getWorker();
        if (worker == null) {
            log.warn("RFID {} scanned but has no worker assigned.", uid);
            return;
        }

        Optional<AttendanceEntity> activeAttendance = attendanceRepository.findActiveAttendanceByWorkerId(worker.getId());

        if (activeAttendance.isPresent()) {
            log.info("Worker {} has active check-in. Processing CHECK-OUT", worker.getId());
            attendanceService.recordCheckOut(request);
        } else {
            log.info("Worker {} has no active check-in. Processing CHECK-IN", worker.getId());
            try {
                attendanceService.recordCheckIn(request);
            } catch (BusinessException e) {
                log.warn("CHECK-IN IGNORED: {}", e.getMessage());
            }
        }
    }
}