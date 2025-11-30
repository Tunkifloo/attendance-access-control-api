package com.iot.attendance.application.service.impl;

import com.iot.attendance.application.dto.request.RfidAttendanceRequest;
import com.iot.attendance.application.dto.response.AttendanceResponse;
import com.iot.attendance.application.mapper.AttendanceMapper;
import com.iot.attendance.application.service.AttendanceService;
import com.iot.attendance.domain.enums.AttendanceStatus;
import com.iot.attendance.infrastructure.exception.BusinessException;
import com.iot.attendance.infrastructure.exception.ResourceNotFoundException;
import com.iot.attendance.infrastructure.firebase.FirebaseRealtimeService;
import com.iot.attendance.infrastructure.persistence.entity.AttendanceEntity;
import com.iot.attendance.infrastructure.persistence.entity.SystemConfigurationEntity;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import com.iot.attendance.infrastructure.persistence.repository.AttendanceRepository;
import com.iot.attendance.infrastructure.persistence.repository.SystemConfigurationRepository;
import com.iot.attendance.infrastructure.persistence.repository.WorkerRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final WorkerRepository workerRepository;
    private final SystemConfigurationRepository configRepository;

    @Getter
    private final AttendanceMapper attendanceMapper;
    private final FirebaseRealtimeService firebaseService;

    @Override
    public AttendanceResponse recordCheckIn(RfidAttendanceRequest request) {
        log.info("Recording check-in for RFID: {}", request.getRfidUid());

        String normalizedRfid = request.getRfidUid().toUpperCase().trim();

        WorkerEntity worker = workerRepository.findByRfidTag(normalizedRfid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with RFID tag: " + normalizedRfid
                ));

        SystemConfigurationEntity config = getCurrentConfiguration();
        LocalDateTime checkInTime = request.getTimestamp() != null ?
                request.getTimestamp() : getCurrentDateTime(config);
        LocalDate attendanceDate = config.getCurrentAttendanceDate() != null ?
                config.getCurrentAttendanceDate() : checkInTime.toLocalDate();

        // Verificar si ya existe un check-in activo
        attendanceRepository.findActiveAttendanceByWorkerId(worker.getId())
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "Worker already has an active check-in for today"
                    );
                });

        AttendanceEntity entity = AttendanceEntity.builder()
                .workerId(worker.getId())
                .rfidTag(normalizedRfid)
                .attendanceDate(attendanceDate)
                .checkInTime(checkInTime)
                .status(AttendanceStatus.CHECKED_IN)
                .build();

        // Calcular tardanza
        calculateLateness(entity, config);

        AttendanceEntity saved = attendanceRepository.save(entity);
        log.info("Check-in recorded for worker {} at {}", worker.getId(), checkInTime);

        // Sincronizar con Firebase
        firebaseService.logAttendance(normalizedRfid, checkInTime, saved.isLate());

        return mapToResponse(saved, worker);
    }

    @Override
    public AttendanceResponse recordCheckOut(RfidAttendanceRequest request) {
        log.info("Recording check-out for RFID: {}", request.getRfidUid());

        String normalizedRfid = request.getRfidUid().toUpperCase().trim();

        WorkerEntity worker = workerRepository.findByRfidTag(normalizedRfid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with RFID tag: " + normalizedRfid
                ));

        AttendanceEntity entity = attendanceRepository.findActiveAttendanceByWorkerId(worker.getId())
                .orElseThrow(() -> new BusinessException(
                        "No active check-in found for worker"
                ));

        SystemConfigurationEntity config = getCurrentConfiguration();
        LocalDateTime checkOutTime = request.getTimestamp() != null ?
                request.getTimestamp() : getCurrentDateTime(config);

        entity.setCheckOutTime(checkOutTime);
        entity.setStatus(AttendanceStatus.CHECKED_OUT);

        // Calcular tiempo trabajado
        Duration workedDuration = Duration.between(entity.getCheckInTime(), checkOutTime);
        entity.setWorkedDuration(workedDuration);
        entity.setUpdatedAt(LocalDateTime.now());

        AttendanceEntity updated = attendanceRepository.save(entity);
        log.info("Check-out recorded for worker {} at {}", worker.getId(), checkOutTime);

        // Sincronizar con Firebase
        firebaseService.logAttendance(normalizedRfid, checkOutTime, false);

        return mapToResponse(updated, worker);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getAttendanceById(Long id) {
        AttendanceEntity entity = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attendance not found with ID: " + id
                ));

        WorkerEntity worker = workerRepository.findById(entity.getWorkerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with ID: " + entity.getWorkerId()
                ));

        return mapToResponse(entity, worker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByDate(LocalDate date) {
        List<AttendanceEntity> entities = attendanceRepository.findByAttendanceDate(date);
        return mapToResponseList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByWorkerAndDateRange(
            Long workerId, LocalDate startDate, LocalDate endDate) {

        List<AttendanceEntity> entities = attendanceRepository
                .findByWorkerIdAndAttendanceDateBetween(workerId, startDate, endDate);

        return mapToResponseList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getLateAttendancesByDate(LocalDate date) {
        List<AttendanceEntity> entities = attendanceRepository.findLateAttendancesByDate(date);
        return mapToResponseList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getActiveAttendanceByWorker(Long workerId) {
        AttendanceEntity entity = attendanceRepository.findActiveAttendanceByWorkerId(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active attendance found for worker: " + workerId
                ));

        WorkerEntity worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with ID: " + workerId
                ));

        return mapToResponse(entity, worker);
    }

    @Override
    @Transactional(readOnly = true)
    public long countLateAttendances(Long workerId, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.countLateAttendances(workerId, startDate, endDate);
    }

    private void calculateLateness(AttendanceEntity entity, SystemConfigurationEntity config) {
        LocalDateTime checkInTime = entity.getCheckInTime();
        LocalDate attendanceDate = entity.getAttendanceDate();

        LocalDateTime workStartDateTime = attendanceDate.atTime(config.getWorkStartTime());

        if (checkInTime.isAfter(workStartDateTime)) {
            entity.setLate(true);
            Duration lateness = Duration.between(workStartDateTime, checkInTime);
            entity.setLatenessDuration(lateness);

            log.info("Worker is late by {} minutes", lateness.toMinutes());
        } else {
            entity.setLate(false);
            entity.setLatenessDuration(Duration.ZERO);
        }
    }

    private SystemConfigurationEntity getCurrentConfiguration() {
        return configRepository.findLatestConfiguration()
                .orElseThrow(() -> new BusinessException(
                        "System configuration not found. Please configure the system first."
                ));
    }

    private LocalDateTime getCurrentDateTime(SystemConfigurationEntity config) {
        if (config.isSimulationMode() && config.getSimulatedDateTime() != null) {
            return config.getSimulatedDateTime();
        }
        return LocalDateTime.now();
    }

    private AttendanceResponse mapToResponse(AttendanceEntity entity, WorkerEntity worker) {
        return AttendanceResponse.builder()
                .id(entity.getId())
                .workerId(entity.getWorkerId())
                .workerFullName(worker.getFirstName() + " " + worker.getLastName())
                .rfidTag(entity.getRfidTag())
                .attendanceDate(entity.getAttendanceDate())
                .checkInTime(entity.getCheckInTime())
                .checkOutTime(entity.getCheckOutTime())
                .workedDuration(formatDuration(entity.getWorkedDuration()))
                .isLate(entity.isLate())
                .latenessDuration(formatDuration(entity.getLatenessDuration()))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private List<AttendanceResponse> mapToResponseList(List<AttendanceEntity> entities) {
        return entities.stream()
                .map(entity -> {
                    WorkerEntity worker = workerRepository.findById(entity.getWorkerId())
                            .orElse(null);
                    return mapToResponse(entity, worker);
                })
                .collect(Collectors.toList());
    }

    private String formatDuration(Duration duration) {
        if (duration == null) {
            return null;
        }
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return String.format("%dh %dm", hours, minutes);
    }

}