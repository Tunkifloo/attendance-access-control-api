package com.iot.attendance.infrastructure.firebase;

import com.iot.attendance.infrastructure.exception.FirebaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseRealtimeService {

    @Value("${firebase.database-url}")
    private String databaseUrl;
    private final RestTemplate restTemplate = new RestTemplate();


    public void logAttendance(String rfidUid, LocalDateTime timestamp, boolean isLate) {
        String url = String.format("%s/logs/asistencia.json", databaseUrl);
        String message = String.format("Marcaje RFID: %s", rfidUid);

        sendPostRequest(url, message, "asistencia");
    }

    public void logAccessGranted(Integer fingerprintId, Long workerId) {
        String url = String.format("%s/logs/accesos.json", databaseUrl);
        String message = String.format("Puerta abierta ID: %d", fingerprintId);

        sendPostRequest(url, message, "acceso concedido");
    }

    public void logAccessDenied(Integer fingerprintId, String reason) {
        String url = String.format("%s/logs/seguridad.json", databaseUrl);
        String message = String.format("Intento fallido huella: %d - %s", fingerprintId, reason);

        sendPostRequest(url, message, "acceso denegado");
    }

    public void setAdminCommand(String command) {
        String url = String.format("%s/admin/comando.json", databaseUrl);
        sendPutRequest(url, command, "comando admin");
    }

    public void setAdminState(String state) {
        String url = String.format("%s/admin/estado.json", databaseUrl);
        sendPutRequest(url, state, "estado admin");
    }

    public void startRegistrationMode() {
        log.info(">> Activando MODO REGISTRO en ESP32...");
        setAdminState("ESPERANDO_REGISTRO");
        setAdminCommand("REGISTRAR");
    }

    public void clearTargetFingerprintId() {
        String url = String.format("%s/admin/id_target.json", databaseUrl);
        try {
            restTemplate.delete(url);
            log.info("✓ id_target eliminado de Firebase");
        } catch (Exception e) {
            log.error("Error limpiando id_target: {}", e.getMessage());
        }
    }

    public void waitForDeletionComplete(int timeoutSeconds) {
        log.info(">> Esperando confirmación de borrado (Timeout: {}s)...", timeoutSeconds);

        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        String lastState = "";

        while (System.currentTimeMillis() < endTime) {
            try {
                String currentState = getAdminStateSync();

                if (currentState != null && !currentState.equals(lastState)) {
                    log.info(">> Estado ESP32: {}", currentState);
                    lastState = currentState;

                    // Hardware confirma borrado exitoso
                    if (currentState.contains("BORRADO EXITOSO") ||
                            currentState.contains("BORRADO_EXITOSO") ||
                            currentState.equals("LISTO")) {

                        log.info("✓ Borrado confirmado por hardware");
                        clearTargetFingerprintId();
                        return;
                    }

                    // Hardware reporta error
                    if (currentState.contains("ERROR") ||
                            currentState.contains("FALLO") ||
                            currentState.contains("BORRADO_FALLO")) {

                        log.warn("⚠ Hardware reportó error en borrado: {}", currentState);
                        clearTargetFingerprintId();
                        throw new FirebaseException("Error en borrado: " + currentState);
                    }
                }

                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new FirebaseException("Espera interrumpida", e);
            }
        }

        // Timeout
        log.warn("⚠ Timeout esperando confirmación de borrado, limpiando id_target...");
        clearTargetFingerprintId();
    }

    public Integer waitForNewFingerprintId(int timeoutSeconds) {
        log.info(">> Esperando a que el usuario registre su huella (Timeout: {}s)...", timeoutSeconds);

        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        String lastState = "";

        while (System.currentTimeMillis() < endTime) {
            try {
                // Leemos el estado actual síncronamente (usando tu método getAdminCommandSync o similar para el estado)
                String currentState = getAdminStateSync();

                if (currentState != null && !currentState.equals(lastState)) {
                    log.info(">> Estado ESP32: {}", currentState);
                    lastState = currentState;

                    // El ESP32 envía "REGISTRO EXITO ID X" al finalizar
                    if (currentState.startsWith("REGISTRO EXITO ID")) {
                        // Extraer el número
                        String idStr = currentState.replace("REGISTRO EXITO ID ", "").trim();
                        return Integer.parseInt(idStr);
                    }

                    if (currentState.contains("FALLO") || currentState.contains("TIMEOUT")) {
                        throw new FirebaseException("El registro falló en el hardware: " + currentState);
                    }
                }

                // Esperar 1 segundo antes de volver a preguntar
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new FirebaseException("Espera interrumpida", e);
            }
        }

        throw new FirebaseException("Tiempo de espera agotado. El usuario no puso el dedo a tiempo.");
    }

    private String getAdminStateSync() {
        String url = String.format("%s/admin/estado.json", databaseUrl);
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.equals("null")) return "";
            if (response.startsWith("\"") && response.endsWith("\"")) {
                return response.substring(1, response.length() - 1);
            }
            return response;
        } catch (Exception e) {
            return "";
        }
    }

    public void setTargetFingerprintId(Integer fingerprintId) {
        String url = String.format("%s/admin/id_target.json", databaseUrl);
        // Los números no llevan comillas en JSON
        sendPutRequestRaw(url, fingerprintId.toString(), "target ID");
    }

    public Integer getLastFingerprintIdSync() {
        String url = String.format("%s/admin/ultimo_id_creado.json", databaseUrl);
        log.info(">> Leyendo ID (REST): {}", url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response != null && !response.equals("null")) {
                return Integer.parseInt(response);
            }
        } catch (Exception e) {
            log.error("Error leyendo lastFingerprintId via REST: {}", e.getMessage());
        }
        return null;
    }

    public String getAdminCommandSync() {
        String url = String.format("%s/admin/comando.json", databaseUrl);
        log.debug(">> Leyendo Comando (REST): {}", url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            // Firebase devuelve el string con comillas (ej: "NADA"), hay que limpiarlas
            return cleanJsonString(response);
        } catch (Exception e) {
            log.error("Error leyendo comando via REST: {}", e.getMessage());
            return "ERROR";
        }
    }

    // Mantenemos la firma para compatibilidad, pero usamos la lógica síncrona dentro
    public CompletableFuture<String> getAdminCommand() {
        return CompletableFuture.supplyAsync(this::getAdminCommandSync);
    }

    public void diagnoseAdminNode() {
        String url = String.format("%s/admin.json", databaseUrl);
        log.info(">> Diagnóstico (REST): {}", url);
        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info("✓ Respuesta Diagnóstico: {}", response);
        } catch (Exception e) {
            log.error("Error en diagnóstico REST: {}", e.getMessage());
        }
    }

    private void sendPostRequest(String url, String message, String logContext) {
        try {
            // En Firebase REST, un string debe ir entre comillas para ser un JSON válido
            String jsonBody = "\"" + message + "\"";
            restTemplate.postForLocation(url, createHttpEntity(jsonBody));
            log.info("✓ Log registrado ({}) via REST", logContext);
        } catch (Exception e) {
            log.error("Error escribiendo {} via REST: {}", logContext, e.getMessage());
        }
    }

    private void sendPutRequest(String url, String value, String logContext) {
        sendPutRequestRaw(url, "\"" + value + "\"", logContext);
    }

    private void sendPutRequestRaw(String url, String jsonBody, String logContext) {
        try {
            restTemplate.put(url, createHttpEntity(jsonBody));
            log.info("✓ {} actualizado a: {}", logContext, jsonBody);
        } catch (Exception e) {
            log.error("Error actualizando {} via REST: {}", logContext, e.getMessage());
        }
    }

    private HttpEntity<String> createHttpEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String cleanJsonString(String response) {
        if (response == null || response.equals("null")) return "NADA";
        // Eliminar comillas iniciales y finales que devuelve la API REST
        if (response.startsWith("\"") && response.endsWith("\"")) {
            return response.substring(1, response.length() - 1);
        }
        return response;
    }
}