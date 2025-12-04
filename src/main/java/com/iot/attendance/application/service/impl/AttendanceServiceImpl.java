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
import com.iot.attendance.infrastructure.persistence.entity.RfidCardEntity;
import com.iot.attendance.infrastructure.persistence.entity.SystemConfigurationEntity;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import com.iot.attendance.infrastructure.persistence.repository.AttendanceRepository;
import com.iot.attendance.infrastructure.persistence.repository.RfidCardRepository;
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
import java.time.LocalTime;
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
    @Getter
    private final FirebaseRealtimeService firebaseService;
    private final RfidCardRepository rfidCardRepository;

    @Override
    public AttendanceResponse recordCheckIn(RfidAttendanceRequest request) {
        log.info("Recording check-in for RFID: {}", request.getRfidUid());

        String normalizedRfid = request.getRfidUid().toUpperCase().replace(" ", "").trim();

        RfidCardEntity card = rfidCardRepository.findById(normalizedRfid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RFID Tag not registered in system: " + normalizedRfid
                ));

        WorkerEntity worker = card.getWorker();
        if (worker == null) {
            throw new ResourceNotFoundException("RFID Tag exists but is not assigned to any worker");
        }

        SystemConfigurationEntity config = getCurrentConfiguration();
        LocalDateTime checkInTime = request.getTimestamp() != null ?
                request.getTimestamp() : getCurrentDateTime(config);
        LocalDate attendanceDate = config.getSimulatedDate() != null && config.isSimulationMode()
                ? config.getSimulatedDate()
                : checkInTime.toLocalDate();

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

        calculateLateness(entity, config);

        AttendanceEntity saved = attendanceRepository.save(entity);

        log.info("✓ Check-in recorded for worker {} ({}) at {} | Late: {} | Duration: {}",
                worker.getId(),
                worker.getFirstName() + " " + worker.getLastName(),
                checkInTime.toLocalTime(),
                saved.isLate() ? "YES" : "NO",
                saved.isLate() ? formatDuration(saved.getLatenessDuration()) : "N/A");

        return mapToResponse(saved, worker);
    }

    @Override
    public AttendanceResponse recordCheckOut(RfidAttendanceRequest request) {
        log.info("Recording check-out for RFID: {}", request.getRfidUid());

        String normalizedRfid = request.getRfidUid().toUpperCase().replace(" ", "").trim();

        RfidCardEntity card = rfidCardRepository.findById(normalizedRfid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RFID Tag not registered in system: " + normalizedRfid
                ));

        WorkerEntity worker = card.getWorker();
        if (worker == null) {
            throw new ResourceNotFoundException("RFID Tag exists but is not assigned to any worker");
        }

        AttendanceEntity entity = attendanceRepository.findActiveAttendanceByWorkerId(worker.getId())
                .orElseThrow(() -> new BusinessException(
                        "No active check-in found for worker"
                ));

        SystemConfigurationEntity config = getCurrentConfiguration();
        LocalDateTime checkOutTime = request.getTimestamp() != null ?
                request.getTimestamp() : getCurrentDateTime(config);

        entity.setCheckOutTime(checkOutTime);
        entity.setStatus(AttendanceStatus.CHECKED_OUT);

        Duration workedDuration = Duration.between(entity.getCheckInTime(), checkOutTime);
        entity.setWorkedDuration(workedDuration);
        entity.setUpdatedAt(LocalDateTime.now());

        AttendanceEntity updated = attendanceRepository.save(entity);

        log.info("✓ Check-out recorded for worker {} ({}) at {} | Worked: {}",
                worker.getId(),
                worker.getFirstName() + " " + worker.getLastName(),
                checkOutTime.toLocalTime(),
                formatDuration(workedDuration));

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

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getLatestAttendanceByWorker(Long workerId) {
        log.info("Fetching latest attendance for worker: {}", workerId);

        AttendanceEntity entity = attendanceRepository.findLatestByWorkerId(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No attendance records found for worker: " + workerId
                ));

        WorkerEntity worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found with ID: " + workerId
                ));

        return mapToResponse(entity, worker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getLatestAttendancesByWorker(Long workerId, int limit) {
        log.info("Fetching latest {} attendances for worker: {}", limit, workerId);

        List<AttendanceEntity> entities = attendanceRepository
                .findLatestByWorkerIdOrderByCreatedAtDesc(workerId);

        List<AttendanceEntity> limitedEntities = entities.stream()
                .limit(limit)
                .collect(Collectors.toList());

        return mapToResponseList(limitedEntities);
    }

    private void calculateLateness(AttendanceEntity entity, SystemConfigurationEntity config) {
        LocalDateTime checkInTime = entity.getCheckInTime();
        LocalDate attendanceDate = entity.getAttendanceDate();
        LocalTime workStartTime = config.getWorkStartTime();
        LocalTime workEndTime = config.getWorkEndTime();

        int toleranceMinutes = config.getLateThresholdMinutes() != null ?
                config.getLateThresholdMinutes() : 0;

        // Determinar si es turno nocturno (cruza medianoche)
        boolean isNightShift = workStartTime.isAfter(workEndTime);

        LocalDateTime workStartDateTime;

        if (isNightShift) {
            // TURNO NOCTURNO:
            // Si check-in es después de medianoche (AM), el turno empezó ayer
            LocalTime checkInOnlyTime = checkInTime.toLocalTime();

            if (checkInOnlyTime.isBefore(workEndTime)) {
                // Check-in en la mañana
                // El turno empezó el día anterior
                workStartDateTime = attendanceDate.minusDays(1).atTime(workStartTime);
            } else {
                // Check-in en la noche
                // El turno empieza hoy
                workStartDateTime = attendanceDate.atTime(workStartTime);
            }
        } else {
            // TURNO DIURNO:
            workStartDateTime = attendanceDate.atTime(workStartTime);
        }

        LocalDateTime lateThresholdTime = workStartDateTime.plusMinutes(toleranceMinutes);

        log.info("Lateness calculation: checkIn={}, workStart={}, threshold={}, isNightShift={}",
                checkInTime.toLocalTime(),
                workStartDateTime.toLocalTime(),
                lateThresholdTime.toLocalTime(),
                isNightShift);

        if (checkInTime.isAfter(lateThresholdTime)) {
            entity.setLate(true);
            Duration lateness = Duration.between(workStartDateTime, checkInTime);
            entity.setLatenessDuration(lateness);

            log.warn("  → Worker is LATE by {} minutes (Tolerance: {} min)",
                    lateness.toMinutes(), toleranceMinutes);
        } else {
            entity.setLate(false);
            entity.setLatenessDuration(Duration.ZERO);

            long minutesEarly = Duration.between(checkInTime, workStartDateTime).toMinutes();
            log.info("  → Worker is ON TIME (arrived {} min early)", minutesEarly);
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
            log.info("Using SIMULATED time: {}", config.getSimulatedDateTime());
            return config.getSimulatedDateTime();
        }
        return LocalDateTime.now();
    }

    private AttendanceResponse mapToResponse(AttendanceEntity entity, WorkerEntity worker) {
        String workerFullName = (worker != null)
                ? worker.getFirstName() + " " + worker.getLastName()
                : "Trabajador Desconocido / Eliminado";

        return AttendanceResponse.builder()
                .id(entity.getId())
                .workerId(entity.getWorkerId())
                .workerFullName(workerFullName)
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
        long seconds = duration.getSeconds() % 60;

        if (hours == 0) {
            if (minutes == 0) {
                return String.format("%ds", seconds);
            }
            return String.format("%dm %ds", minutes, seconds);
        }

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}