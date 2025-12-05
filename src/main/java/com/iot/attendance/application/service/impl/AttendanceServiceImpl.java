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
import org.springframework.data.domain.Sort;
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
@Transactional(noRollbackFor = BusinessException.class)
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
                .orElseThrow(() -> new ResourceNotFoundException("RFID Tag not registered: " + normalizedRfid));

        WorkerEntity worker = card.getWorker();
        if (worker == null) {
            throw new ResourceNotFoundException("RFID Tag unassigned");
        }

        SystemConfigurationEntity config = getCurrentConfiguration();
        LocalDateTime checkInTime = request.getTimestamp() != null ? request.getTimestamp() : getCurrentDateTime(config);

        LocalTime nowTime = checkInTime.toLocalTime();
        LocalTime workStart = config.getWorkStartTime();
        LocalTime workEnd = config.getWorkEndTime();

        LocalTime windowStart = workStart.minusMinutes(60);
        LocalTime windowEnd = workEnd;

        boolean isWindowCrossingMidnight = windowStart.isAfter(windowEnd);
        boolean isWithinHours;

        if (isWindowCrossingMidnight) {
            isWithinHours = !nowTime.isBefore(windowStart) || !nowTime.isAfter(windowEnd);
        } else {
            isWithinHours = !nowTime.isBefore(windowStart) && !nowTime.isAfter(windowEnd);
        }

        if (!isWithinHours) {
            log.warn("Check-in RECHAZADO: Hora {} fuera del rango permitido ({} - {})",
                    nowTime, windowStart, windowEnd);
            throw new BusinessException("Fuera de horario permitido para entrada.");
        }

        attendanceRepository.findActiveAttendanceByWorkerId(worker.getId())
                .ifPresent(existing -> { throw new BusinessException("Worker already has an active check-in"); });

        LocalDate attendanceDate = config.getSimulatedDate() != null && config.isSimulationMode() ? config.getSimulatedDate() : checkInTime.toLocalDate();

        AttendanceEntity entity = AttendanceEntity.builder()
                .workerId(worker.getId())
                .workerSnapshotName(worker.getFirstName() + " " + worker.getLastName())
                .rfidTag(normalizedRfid)
                .attendanceDate(attendanceDate)
                .checkInTime(checkInTime)
                .status(AttendanceStatus.CHECKED_IN)
                .build();

        calculateLateness(entity, config);

        AttendanceEntity saved = attendanceRepository.save(entity);
        return mapToResponse(saved, worker);
    }

    @Override
    public AttendanceResponse recordCheckOut(RfidAttendanceRequest request) {
        log.info("Recording check-out for RFID: {}", request.getRfidUid());

        String normalizedRfid = request.getRfidUid().toUpperCase().replace(" ", "").trim();
        RfidCardEntity card = rfidCardRepository.findById(normalizedRfid)
                .orElseThrow(() -> new ResourceNotFoundException("RFID Tag not registered"));

        WorkerEntity worker = card.getWorker();
        if (worker == null) throw new ResourceNotFoundException("RFID Tag unassigned");

        AttendanceEntity entity = attendanceRepository.findActiveAttendanceByWorkerId(worker.getId())
                .orElseThrow(() -> new BusinessException("No active check-in found"));

        SystemConfigurationEntity config = getCurrentConfiguration();
        LocalDateTime checkOutTime = request.getTimestamp() != null ? request.getTimestamp() : getCurrentDateTime(config);

        entity.setCheckOutTime(checkOutTime);
        entity.setStatus(AttendanceStatus.CHECKED_OUT);
        entity.setWorkedDuration(Duration.between(entity.getCheckInTime(), checkOutTime));
        entity.setUpdatedAt(LocalDateTime.now());

        AttendanceEntity updated = attendanceRepository.save(entity);
        return mapToResponse(updated, worker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceHistory(LocalDate startDate, LocalDate endDate, String status, String sortDirection) {
        Sort sort = Sort.by("checkInTime");
        if ("ASC".equalsIgnoreCase(sortDirection)) {
            sort = sort.ascending();
        } else {
            sort = sort.descending();
        }

        List<AttendanceEntity> entities;
        if ("LATE".equalsIgnoreCase(status)) {
            entities = attendanceRepository.findByAttendanceDateBetweenAndIsLate(startDate, endDate, true, sort);
        } else if ("ON_TIME".equalsIgnoreCase(status)) {
            entities = attendanceRepository.findByAttendanceDateBetweenAndIsLate(startDate, endDate, false, sort);
        } else {
            entities = attendanceRepository.findByAttendanceDateBetween(startDate, endDate, sort);
        }
        return mapToResponseList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getAttendanceById(Long id) {
        AttendanceEntity entity = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found: " + id));
        WorkerEntity worker = entity.getWorkerId() != null ? workerRepository.findById(entity.getWorkerId()).orElse(null) : null;
        return mapToResponse(entity, worker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByDate(LocalDate date) {
        return mapToResponseList(attendanceRepository.findByAttendanceDate(date));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByWorkerAndDateRange(Long workerId, LocalDate startDate, LocalDate endDate) {
        return mapToResponseList(attendanceRepository.findByWorkerIdAndAttendanceDateBetween(workerId, startDate, endDate));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getLateAttendancesByDate(LocalDate date) {
        return mapToResponseList(attendanceRepository.findLateAttendancesByDate(date));
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getActiveAttendanceByWorker(Long workerId) {
        AttendanceEntity entity = attendanceRepository.findActiveAttendanceByWorkerId(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("No active attendance found"));
        WorkerEntity worker = workerRepository.findById(workerId).orElse(null);
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
        AttendanceEntity entity = attendanceRepository.findLatestByWorkerId(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("No attendance records found"));
        WorkerEntity worker = workerRepository.findById(workerId).orElse(null);
        return mapToResponse(entity, worker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getLatestAttendancesByWorker(Long workerId, int limit) {
        return mapToResponseList(attendanceRepository.findLatestByWorkerIdOrderByCreatedAtDesc(workerId)
                .stream().limit(limit).collect(Collectors.toList()));
    }

    private void calculateLateness(AttendanceEntity entity, SystemConfigurationEntity config) {
        LocalDateTime checkInTime = entity.getCheckInTime();
        LocalDate attendanceDate = entity.getAttendanceDate();
        LocalTime workStartTime = config.getWorkStartTime();
        LocalTime workEndTime = config.getWorkEndTime();

        int toleranceMinutes = config.getLateThresholdMinutes() != null ? config.getLateThresholdMinutes() : 0;

        LocalDateTime workStartDateTime;
        if (workStartTime.isAfter(workEndTime)) {
            if (checkInTime.toLocalTime().isBefore(workEndTime)) {
                workStartDateTime = attendanceDate.minusDays(1).atTime(workStartTime);
            } else {
                workStartDateTime = attendanceDate.atTime(workStartTime);
            }
        } else {
            workStartDateTime = attendanceDate.atTime(workStartTime);
        }

        LocalDateTime lateThresholdTime = workStartDateTime.plusMinutes(toleranceMinutes);

        if (checkInTime.isAfter(lateThresholdTime)) {
            entity.setLate(true);
            entity.setLatenessDuration(Duration.between(workStartDateTime, checkInTime));
        } else {
            entity.setLate(false);
            entity.setLatenessDuration(Duration.ZERO);
        }
    }

    private SystemConfigurationEntity getCurrentConfiguration() {
        return configRepository.findLatestConfiguration()
                .orElseThrow(() -> new BusinessException("System configuration not found"));
    }

    private LocalDateTime getCurrentDateTime(SystemConfigurationEntity config) {
        if (config.isSimulationMode() && config.getSimulatedDateTime() != null) {
            return config.getSimulatedDateTime();
        }
        return LocalDateTime.now();
    }

    private AttendanceResponse mapToResponse(AttendanceEntity entity, WorkerEntity worker) {
        String workerFullName = "Desconocido";
        if (worker != null) {
            workerFullName = worker.getFirstName() + " " + worker.getLastName();
        } else if (entity.getWorkerSnapshotName() != null) {
            workerFullName = entity.getWorkerSnapshotName() + " (Eliminado)";
        }

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
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private List<AttendanceResponse> mapToResponseList(List<AttendanceEntity> entities) {
        return entities.stream()
                .map(entity -> {
                    WorkerEntity worker = entity.getWorkerId() != null ? workerRepository.findById(entity.getWorkerId()).orElse(null) : null;
                    return mapToResponse(entity, worker);
                })
                .collect(Collectors.toList());
    }

    private String formatDuration(Duration duration) {
        if (duration == null) return null;
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        if (hours == 0) {
            return minutes == 0 ? String.format("%ds", seconds) : String.format("%dm %ds", minutes, seconds);
        }
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}