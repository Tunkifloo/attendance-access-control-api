package com.iot.attendance.presentation.controller;

import com.iot.attendance.application.dto.request.RfidAttendanceRequest;
import com.iot.attendance.application.dto.response.ApiResponse;
import com.iot.attendance.application.dto.response.AttendanceResponse;
import com.iot.attendance.application.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Attendance", description = "Control de asistencia mediante RFID")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @Operation(summary = "Registrar entrada (check-in)",
            description = "Registra la hora de entrada de un trabajador mediante RFID")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(
            @Valid @RequestBody RfidAttendanceRequest request) {

        log.info("Processing check-in for RFID: {}", request.getRfidUid());
        AttendanceResponse response = attendanceService.recordCheckIn(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Check-in recorded successfully", response));
    }

    @PostMapping("/check-out")
    @Operation(summary = "Registrar salida (check-out)",
            description = "Registra la hora de salida de un trabajador mediante RFID")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(
            @Valid @RequestBody RfidAttendanceRequest request) {

        log.info("Processing check-out for RFID: {}", request.getRfidUid());
        AttendanceResponse response = attendanceService.recordCheckOut(request);

        return ResponseEntity.ok(ApiResponse.success("Check-out recorded successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener asistencia por ID")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getAttendanceById(
            @Parameter(description = "ID de asistencia") @PathVariable Long id) {

        AttendanceResponse response = attendanceService.getAttendanceById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/date/{date}")
    @Operation(summary = "Obtener asistencias por fecha",
            description = "Lista todas las asistencias de una fecha específica")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendanceByDate(
            @Parameter(description = "Fecha (formato: yyyy-MM-dd)")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<AttendanceResponse> responses = attendanceService.getAttendanceByDate(date);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/date/{date}/late")
    @Operation(summary = "Obtener tardanzas por fecha",
            description = "Lista los trabajadores que llegaron tarde en una fecha específica")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getLateAttendances(
            @Parameter(description = "Fecha (formato: yyyy-MM-dd)")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<AttendanceResponse> responses = attendanceService.getLateAttendancesByDate(date);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/worker/{workerId}")
    @Operation(summary = "Obtener asistencias por trabajador y rango de fechas")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendanceByWorkerAndRange(
            @Parameter(description = "ID del trabajador") @PathVariable Long workerId,
            @Parameter(description = "Fecha inicial")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha final")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<AttendanceResponse> responses = attendanceService
                .getAttendanceByWorkerAndDateRange(workerId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/worker/{workerId}/active")
    @Operation(summary = "Obtener asistencia activa del trabajador",
            description = "Retorna la asistencia en curso (check-in sin check-out)")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getActiveAttendance(
            @Parameter(description = "ID del trabajador") @PathVariable Long workerId) {

        AttendanceResponse response = attendanceService.getActiveAttendanceByWorker(workerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/worker/{workerId}/late-count")
    @Operation(summary = "Contar tardanzas de un trabajador")
    public ResponseEntity<ApiResponse<Long>> countLateAttendances(
            @Parameter(description = "ID del trabajador") @PathVariable Long workerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        long count = attendanceService.countLateAttendances(workerId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Worker has %d late attendances in the specified period", count),
                count
        ));
    }

    @GetMapping("/worker/{workerId}/latest")
    @Operation(summary = "Obtener última asistencia del trabajador",
            description = "Retorna el registro de asistencia más reciente del trabajador")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getLatestAttendance(
            @Parameter(description = "ID del trabajador") @PathVariable Long workerId) {

        log.info("Fetching latest attendance for worker: {}", workerId);
        AttendanceResponse response = attendanceService.getLatestAttendanceByWorker(workerId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/worker/{workerId}/recent")
    @Operation(summary = "Obtener últimas N asistencias del trabajador",
            description = "Retorna los últimos N registros de asistencia del trabajador")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getRecentAttendances(
            @Parameter(description = "ID del trabajador") @PathVariable Long workerId,
            @Parameter(description = "Número de registros a retornar")
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Fetching latest {} attendances for worker: {}", limit, workerId);
        List<AttendanceResponse> responses = attendanceService
                .getLatestAttendancesByWorker(workerId, limit);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
