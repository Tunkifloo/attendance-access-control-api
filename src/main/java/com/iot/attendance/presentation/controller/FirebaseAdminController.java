package com.iot.attendance.presentation.controller;

import com.iot.attendance.application.dto.response.ApiResponse;
import com.iot.attendance.infrastructure.firebase.FirebaseRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/firebase/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Firebase Admin", description = "Comandos de administración para dispositivos IoT")
public class FirebaseAdminController {

    private final FirebaseRealtimeService firebaseService;

    @GetMapping("/diagnose")
    @Operation(summary = "Diagnóstico completo del nodo admin",
            description = "Lee todo el nodo admin para debugging")
    public ResponseEntity<ApiResponse<Map<String, String>>> diagnose() {
        log.info("Ejecutando diagnóstico de Firebase");

        firebaseService.diagnoseAdminNode();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Check server logs for detailed diagnostics");

        return ResponseEntity.ok(ApiResponse.success("Diagnostics completed", response));
    }

    @GetMapping("/command")
    @Operation(summary = "Obtener comando actual",
            description = "Lee el comando actual configurado en Firebase para el ESP32")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCurrentCommand() {
        CompletableFuture<String> commandFuture = firebaseService.getAdminCommand();

        String command = commandFuture.join();

        Map<String, String> response = new HashMap<>();
        response.put("command", command);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/command/register")
    @Operation(summary = "Comando: Registrar nueva huella",
            description = "Envía comando al ESP32 para iniciar proceso de registro de huella")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendRegisterCommand() {
        log.info("Sending REGISTER command to ESP32");

        firebaseService.setAdminCommand("REGISTRAR");
        firebaseService.setAdminState("ESPERANDO_REGISTRO");

        Map<String, String> response = new HashMap<>();
        response.put("command", "REGISTRAR");
        response.put("message", "Command sent to ESP32. Please place finger on sensor.");

        return ResponseEntity.ok(ApiResponse.success(
                "Register command sent successfully",
                response
        ));
    }

    @PostMapping("/command/delete")
    @Operation(summary = "Comando: Borrar huella",
            description = "Envía comando al ESP32 para eliminar una huella específica")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendDeleteCommand(
            @Parameter(description = "ID de huella a eliminar")
            @RequestParam Integer fingerprintId) {

        log.info("Sending DELETE command for fingerprint ID: {}", fingerprintId);

        firebaseService.setTargetFingerprintId(fingerprintId);
        firebaseService.setAdminCommand("BORRAR");
        firebaseService.setAdminState("BORRANDO");

        Map<String, String> response = new HashMap<>();
        response.put("command", "BORRAR");
        response.put("targetId", String.valueOf(fingerprintId));
        response.put("message", "Delete command sent to ESP32");

        return ResponseEntity.ok(ApiResponse.success(
                "Delete command sent successfully",
                response
        ));
    }

    @PostMapping("/command/format")
    @Operation(summary = "Comando: Formatear sensor",
            description = "Envía comando al ESP32 para borrar todas las huellas registradas")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendFormatCommand() {
        log.warn("Sending FORMAT command - This will delete ALL fingerprints!");

        firebaseService.setAdminCommand("FORMATEAR");
        firebaseService.setAdminState("FORMATEANDO");

        Map<String, String> response = new HashMap<>();
        response.put("command", "FORMATEAR");
        response.put("message", "Format command sent to ESP32. All fingerprints will be deleted.");

        return ResponseEntity.ok(ApiResponse.success(
                "Format command sent successfully",
                response
        ));
    }

    @PostMapping("/command/clear")
    @Operation(summary = "Limpiar comando",
            description = "Resetea el comando a NADA")
    public ResponseEntity<ApiResponse<Map<String, String>>> clearCommand() {
        log.info("Clearing admin command");

        firebaseService.setAdminCommand("NADA");
        firebaseService.setAdminState("LISTO");

        Map<String, String> response = new HashMap<>();
        response.put("command", "NADA");
        response.put("message", "Command cleared");

        return ResponseEntity.ok(ApiResponse.success(
                "Command cleared successfully",
                response
        ));
    }

    @GetMapping("/last-fingerprint-id")
    @Operation(summary = "Obtener último ID de huella creado",
            description = "Consulta el ID de la última huella registrada por el ESP32")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getLastFingerprintId() {
        Integer lastId = firebaseService.getLastFingerprintIdSync();

        Map<String, Integer> response = new HashMap<>();
        response.put("lastFingerprintId", lastId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/state")
    @Operation(summary = "Actualizar estado del sistema")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateState(
            @RequestParam String state) {

        log.info("Updating admin state to: {}", state);
        firebaseService.setAdminState(state);

        Map<String, String> response = new HashMap<>();
        response.put("state", state);

        return ResponseEntity.ok(ApiResponse.success("State updated", response));
    }
}
