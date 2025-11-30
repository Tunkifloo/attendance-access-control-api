package com.iot.attendance.presentation.controller;

import com.iot.attendance.application.dto.response.ApiResponse;
import com.iot.attendance.application.dto.response.SecurityLogResponse;
import com.iot.attendance.application.service.SecurityService;
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
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Security", description = "Logs de seguridad y alertas")
public class SecurityController {

    private final SecurityService securityService;

    @GetMapping("/logs")
    @Operation(summary = "Obtener logs de seguridad por rango de tiempo")
    public ResponseEntity<ApiResponse<List<SecurityLogResponse>>> getSecurityLogs(
            @Parameter(description = "Fecha/hora inicial")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "Fecha/hora final")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<SecurityLogResponse> responses = securityService
                .getSecurityLogsByTimeRange(startTime, endTime);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/critical")
    @Operation(summary = "Obtener eventos críticos de seguridad")
    public ResponseEntity<ApiResponse<List<SecurityLogResponse>>> getCriticalEvents() {
        List<SecurityLogResponse> responses = securityService.getCriticalEvents();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/events/{eventType}")
    @Operation(summary = "Obtener eventos por tipo")
    public ResponseEntity<ApiResponse<List<SecurityLogResponse>>> getEventsByType(
            @Parameter(description = "Tipo de evento") @PathVariable String eventType,
            @Parameter(description = "Número de horas hacia atrás")
            @RequestParam(defaultValue = "24") int hours) {

        List<SecurityLogResponse> responses = securityService
                .getRecentEventsByType(eventType, hours);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/manual-log")
    @Operation(summary = "Registrar evento de seguridad manual")
    public ResponseEntity<ApiResponse<Void>> logSecurityEvent(
            @RequestParam String eventType,
            @RequestParam String description,
            @RequestParam(defaultValue = "MEDIUM") String severity) {

        log.info("Manual security log: {} - {}", eventType, description);
        securityService.logSecurityEvent(eventType, description, severity);

        return ResponseEntity.ok(ApiResponse.success("Security event logged successfully", null));
    }
}