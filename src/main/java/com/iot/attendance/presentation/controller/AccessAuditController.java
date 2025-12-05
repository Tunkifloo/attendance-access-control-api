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
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> getAccessHistoryByWorker(
            @PathVariable Long workerId) {
        List<AccessLogResponse> responses = accessAuditService.getAccessHistoryByWorker(workerId);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Worker ID %d has %d records", workerId, responses.size()),
                responses
        ));
    }

    @GetMapping("/time-range")
    @Operation(summary = "Obtener accesos con filtros", description = "Permite filtrar por rango, estado y ordenar")
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> getAccessHistoryByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "Filtro de estado (GRANTED, DENIED, ALL)")
            @RequestParam(defaultValue = "ALL") String status,
            @Parameter(description = "Ordenamiento (ASC, DESC)")
            @RequestParam(defaultValue = "DESC") String sort) {

        List<AccessLogResponse> responses = accessAuditService
                .getAccessHistoryByTimeRange(startTime, endTime, status, sort);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d records (Filter: %s, Order: %s)", responses.size(), status, sort),
                responses
        ));
    }

    @GetMapping("/denied")
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> getRecentDeniedAccess(
            @RequestParam(defaultValue = "24") int hours) {
        List<AccessLogResponse> responses = accessAuditService.getRecentDeniedAccess(hours);
        return ResponseEntity.ok(ApiResponse.success("Recent denied count: " + responses.size(), responses));
    }

    @GetMapping("/granted")
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> getRecentGrantedAccess(
            @RequestParam(defaultValue = "24") int hours) {
        List<AccessLogResponse> responses = accessAuditService.getRecentGrantedAccess(hours);
        return ResponseEntity.ok(ApiResponse.success("Recent granted count: " + responses.size(), responses));
    }

    @GetMapping("/worker/{workerId}/denied-count")
    public ResponseEntity<ApiResponse<Long>> countDeniedAccesses(
            @PathVariable Long workerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        long count = accessAuditService.countDeniedAccessesByWorker(workerId, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success("Count: " + count, count));
    }
}