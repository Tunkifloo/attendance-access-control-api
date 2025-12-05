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
    @Operation(summary = "Obtener logs de seguridad", description = "Filtra por rango de fecha y tipo (ACCESS, ATTENDANCE)")
    public ResponseEntity<ApiResponse<List<SecurityLogResponse>>> getSecurityLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "Tipo (ACCESS, ATTENDANCE, ALL)")
            @RequestParam(defaultValue = "ALL") String severity,
            @Parameter(description = "Ordenamiento (ASC, DESC)")
            @RequestParam(defaultValue = "DESC") String sort) {

        List<SecurityLogResponse> responses = securityService
                .getSecurityLogsByTimeRange(startTime, endTime, severity, sort);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d security events", responses.size()),
                responses
        ));
    }

    @PostMapping("/manual-log")
    public ResponseEntity<ApiResponse<Void>> logSecurityEvent(
            @RequestParam String eventType,
            @RequestParam String description,
            @RequestParam(defaultValue = "ACCESS") String severity) {

        securityService.logSecurityEvent(eventType, description, severity);
        return ResponseEntity.ok(ApiResponse.success("Security event logged", null));
    }
}