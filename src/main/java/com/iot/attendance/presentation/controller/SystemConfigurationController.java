package com.iot.attendance.presentation.controller;

import com.iot.attendance.application.dto.request.UpdateSystemConfigRequest;
import com.iot.attendance.application.dto.response.ApiResponse;
import com.iot.attendance.application.dto.response.SystemConfigurationResponse;
import com.iot.attendance.application.service.SystemConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System Configuration", description = "Configuración del sistema")
public class SystemConfigurationController {

    private final SystemConfigurationService configService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    @Operation(summary = "Obtener configuración actual del sistema")
    public ResponseEntity<ApiResponse<SystemConfigurationResponse>> getCurrentConfiguration() {
        SystemConfigurationResponse response = configService.getCurrentConfiguration();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    @Operation(summary = "Actualizar configuración del sistema")
    public ResponseEntity<ApiResponse<SystemConfigurationResponse>> updateConfiguration(
            @Valid @RequestBody UpdateSystemConfigRequest request) {

        log.info("Updating system configuration");
        SystemConfigurationResponse response = configService.updateConfiguration(request);

        return ResponseEntity.ok(ApiResponse.success(
                "System configuration updated successfully",
                response
        ));
    }

    @PostMapping("/initialize")
    @Operation(summary = "Inicializar configuración por defecto",
            description = "Crea la configuración inicial del sistema con valores por defecto")
    public ResponseEntity<ApiResponse<SystemConfigurationResponse>> initializeConfiguration() {
        log.info("Initializing default system configuration");
        SystemConfigurationResponse response = configService.initializeDefaultConfiguration();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Default configuration initialized successfully",
                        response
                ));
    }

    @PostMapping("/simulation/enable")
    @Operation(summary = "Habilitar modo simulación")
    public ResponseEntity<ApiResponse<SystemConfigurationResponse>> enableSimulation(
            @Valid @RequestBody UpdateSystemConfigRequest request) {
        log.info("Enabling simulation mode");
        SystemConfigurationResponse response = configService.enableSimulationMode(request);
        return ResponseEntity.ok(ApiResponse.success("Simulation mode enabled", response));
    }

    @PostMapping("/simulation/disable")
    @Operation(summary = "Deshabilitar modo simulación")
    public ResponseEntity<ApiResponse<SystemConfigurationResponse>> disableSimulation() {
        log.info("Disabling simulation mode");
        SystemConfigurationResponse response = configService.disableSimulationMode();
        return ResponseEntity.ok(ApiResponse.success("Simulation mode disabled", response));
    }

    @DeleteMapping("/purge-history")
    @Operation(summary = "PURGAR HISTORIAL", description = "Borra todos los logs de acceso y asistencias. Mantiene usuarios.")
    public ResponseEntity<ApiResponse<Void>> purgeHistory() {
        log.warn("PURGING ALL HISTORY DATA REQUESTED");

        jdbcTemplate.execute("TRUNCATE TABLE access_logs, attendances RESTART IDENTITY");

        return ResponseEntity.ok(ApiResponse.success("Historial eliminado correctamente", null));
    }
}