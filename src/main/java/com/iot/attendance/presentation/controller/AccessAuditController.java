package com.iot.attendance.presentation.controller;

import com.iot.attendance.application.dto.response.AccessLogResponse;
import com.iot.attendance.application.dto.response.ApiResponse;
import com.iot.attendance.application.service.AccessAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/access-audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Access Audit", description = "Auditoría de accesos registrados por hardware ESP32")
public class AccessAuditController {

    private final AccessAuditService accessAuditService;

    @GetMapping("/worker/{workerId}")
    @Operation(summary = "Obtener historial de accesos de un trabajador",
            description = "Retorna todos los intentos de acceso (exitosos y fallidos) de un trabajador")
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> getAccessHistoryByWorker(
            @Parameter(description = "ID del trabajador")
            @PathVariable Long workerId) {

        log.info("GET /access-audit/worker/{}", workerId);

        List<AccessLogResponse> responses = accessAuditService.getAccessHistoryByWorker(workerId);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d access logs for worker %d", responses.size(), workerId),
                responses
        ));
    }

    @GetMapping("/time-range")
    @Operation(summary = "Obtener accesos por rango de tiempo",
            description = "Filtra logs de acceso en un período específico")
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> getAccessHistoryByTimeRange(
            @Parameter(description = "Fecha/hora inicial (ISO 8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "Fecha/hora final (ISO 8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("GET /access-audit/time-range?startTime={}&endTime={}", startTime, endTime);

        List<AccessLogResponse> responses = accessAuditService
                .getAccessHistoryByTimeRange(startTime, endTime);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/denied")
    @Operation(summary = "Obtener accesos denegados recientes",
            description = "Lista intentos de acceso fallidos en las últimas N horas")
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> getRecentDeniedAccess(
            @Parameter(description = "Número de horas hacia atrás (default: 24)")
            @RequestParam(defaultValue = "24") int hours) {

        log.info("GET /access-audit/denied?hours={}", hours);

        List<AccessLogResponse> responses = accessAuditService.getRecentDeniedAccess(hours);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d denied accesses in last %d hours", responses.size(), hours),
                responses
        ));
    }

    @GetMapping("/granted")
    @Operation(summary = "Obtener accesos concedidos recientes",
            description = "Lista accesos exitosos en las últimas N horas")
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> getRecentGrantedAccess(
            @Parameter(description = "Número de horas hacia atrás (default: 24)")
            @RequestParam(defaultValue = "24") int hours) {

        log.info("GET /access-audit/granted?hours={}", hours);

        List<AccessLogResponse> responses = accessAuditService.getRecentGrantedAccess(hours);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d granted accesses in last %d hours", responses.size(), hours),
                responses
        ));
    }

    @GetMapping("/worker/{workerId}/denied-count")
    @Operation(summary = "Contar accesos denegados de un trabajador",
            description = "Cuenta intentos fallidos en un período específico")
    public ResponseEntity<ApiResponse<Long>> countDeniedAccesses(
            @Parameter(description = "ID del trabajador")
            @PathVariable Long workerId,
            @Parameter(description = "Fecha/hora inicial")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "Fecha/hora final")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("GET /access-audit/worker/{}/denied-count", workerId);

        long count = accessAuditService.countDeniedAccessesByWorker(workerId, startTime, endTime);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Worker has %d denied accesses in the specified period", count),
                count
        ));
    }
}