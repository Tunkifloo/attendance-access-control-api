package com.iot.attendance.presentation.controller;

import com.iot.attendance.application.dto.request.FingerprintAccessRequest;
import com.iot.attendance.application.dto.response.AccessLogResponse;
import com.iot.attendance.application.dto.response.ApiResponse;
import com.iot.attendance.application.service.AccessControlService;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/access")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Access Control", description = "Control de acceso mediante huella dactilar")
public class AccessControlController {

    private final AccessControlService accessControlService;

    @PostMapping("/verify")
    @Operation(summary = "Verificar acceso",
            description = "Procesa una solicitud de acceso mediante huella dactilar")
    public ResponseEntity<ApiResponse<AccessLogResponse>> verifyAccess(
            @Valid @RequestBody FingerprintAccessRequest request) {

        log.info("Processing access verification for fingerprint: {}", request.getFingerprintId());
        AccessLogResponse response = accessControlService.processAccess(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Access request processed", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener log de acceso por ID")
    public ResponseEntity<ApiResponse<AccessLogResponse>> getAccessLogById(
            @Parameter(description = "ID del log de acceso") @PathVariable Long id) {

        AccessLogResponse response = accessControlService.getAccessLogById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/worker/{workerId}")
    @Operation(summary = "Obtener logs de acceso por trabajador")
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> getAccessLogsByWorker(
            @Parameter(description = "ID del trabajador") @PathVariable Long workerId) {

        List<AccessLogResponse> responses = accessControlService.getAccessLogsByWorker(workerId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/time-range")
    @Operation(summary = "Obtener logs de acceso por rango de tiempo")
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> getAccessLogsByTimeRange(
            @Parameter(description = "Fecha/hora inicial")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "Fecha/hora final")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<AccessLogResponse> responses = accessControlService
                .getAccessLogsByTimeRange(startTime, endTime);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/denied")
    @Operation(summary = "Obtener accesos denegados recientes",
            description = "Lista los intentos de acceso denegados en las últimas horas")
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> getRecentDeniedAccess(
            @Parameter(description = "Número de horas hacia atrás")
            @RequestParam(defaultValue = "24") int hours) {

        List<AccessLogResponse> responses = accessControlService.getRecentDeniedAccess(hours);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
