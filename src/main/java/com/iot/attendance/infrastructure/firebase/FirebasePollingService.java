package com.iot.attendance.infrastructure.firebase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.attendance.application.dto.request.RfidAttendanceRequest;
import com.iot.attendance.application.service.AccessAuditService;
import com.iot.attendance.application.service.SecurityService;
import com.iot.attendance.application.service.impl.SmartAttendanceProcessor;
import com.iot.attendance.infrastructure.persistence.entity.RfidCardEntity;
import com.iot.attendance.infrastructure.persistence.repository.RfidCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebasePollingService {

    @Value("${firebase.database-url}")
    private String databaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RfidCardRepository rfidCardRepository;
    private final SmartAttendanceProcessor smartAttendanceProcessor;
    private final AccessAuditService accessAuditService;
    private final SecurityService securityService;

    private static final Pattern RFID_PATTERN = Pattern.compile("Marcaje RFID: ([A-F0-9 ]+)");
    private static final Pattern ACCESS_GRANTED_PATTERN = Pattern.compile("Puerta abierta ID: (\\d+)");
    private static final Pattern ACCESS_DENIED_PATTERN = Pattern.compile("Intento fallido huella|Huella desconocida");

    private final Set<String> processedAttendanceKeys = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> processedAccessKeys = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> processedSecurityKeys = Collections.synchronizedSet(new HashSet<>());

    @Scheduled(fixedRate = 3000)
    public void pollRecentAttendance() {
        try {
            String url = String.format("%s/logs/asistencia.json?orderBy=\"$key\"&limitToLast=5", databaseUrl);
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.equals("null")) return;

            JsonNode rootNode = objectMapper.readTree(response);
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                String message = entry.getValue().asText();

                if (!processedAttendanceKeys.contains(key)) {
                    processAttendanceMessage(message);
                    processedAttendanceKeys.add(key);
                    if (processedAttendanceKeys.size() > 1000) processedAttendanceKeys.clear();
                }
            }
        } catch (Exception e) {
            log.error("Error polling asistencia: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3000)
    public void pollAccessLogs() {
        try {
            String url = String.format("%s/logs/accesos.json?orderBy=\"$key\"&limitToLast=5", databaseUrl);
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.equals("null")) return;

            JsonNode rootNode = objectMapper.readTree(response);
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                String message = entry.getValue().asText();

                if (!processedAccessKeys.contains(key)) {
                    processAccessGrantedMessage(message);
                    processedAccessKeys.add(key);
                    if (processedAccessKeys.size() > 1000) processedAccessKeys.clear();
                }
            }
        } catch (Exception e) {
            log.error("Error polling accesos: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3000)
    public void pollSecurityLogs() {
        try {
            String url = String.format("%s/logs/seguridad.json?orderBy=\"$key\"&limitToLast=5", databaseUrl);
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.equals("null")) return;

            JsonNode rootNode = objectMapper.readTree(response);
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                String message = entry.getValue().asText();

                if (!processedSecurityKeys.contains(key)) {
                    processAccessDeniedMessage(message);
                    processedSecurityKeys.add(key);
                    if (processedSecurityKeys.size() > 1000) processedSecurityKeys.clear();
                }
            }
        } catch (Exception e) {
            log.error("Error polling seguridad: {}", e.getMessage());
        }
    }

    private void processAttendanceMessage(String message) {
        Matcher matcher = RFID_PATTERN.matcher(message);
        if (matcher.find()) {
            String rawUid = matcher.group(1);
            String normalizedUid = rawUid.toUpperCase().replace(" ", "").trim();
            Optional<RfidCardEntity> cardOpt = rfidCardRepository.findById(normalizedUid);

            if (cardOpt.isEmpty()) {
                log.warn("⚠ RFID NO REGISTRADO: {}", normalizedUid);
                // USAMOS SEVERITY COMO "ATTENDANCE"
                securityService.logSecurityEvent("UNKNOWN_RFID", "Tarjeta desconocida: " + normalizedUid, "ATTENDANCE");
                return;
            }

            RfidCardEntity card = cardOpt.get();
            card.setLastSeen(LocalDateTime.now());
            rfidCardRepository.save(card);

            if (card.getWorker() != null) {
                processCheckInCheckOut(normalizedUid);
            }
        }
    }

    private void processAccessGrantedMessage(String message) {
        Matcher matcher = ACCESS_GRANTED_PATTERN.matcher(message);
        if (matcher.find()) {
            accessAuditService.logAccessGranted(Integer.parseInt(matcher.group(1)), LocalDateTime.now());
        }
    }

    private void processAccessDeniedMessage(String message) {
        if (ACCESS_DENIED_PATTERN.matcher(message).find()) {
            log.info(">> [ACCESO DENEGADO]");
            accessAuditService.logAccessDenied(null, LocalDateTime.now());
            // USAMOS SEVERITY COMO "ACCESS"
            securityService.logSecurityEvent("ACCESS_DENIED", "Huella no reconocida", "ACCESS");
        }
    }

    private void processCheckInCheckOut(String rfidUid) {
        try {
            RfidAttendanceRequest request = RfidAttendanceRequest.builder()
                    .rfidUid(rfidUid).timestamp(LocalDateTime.now()).build();
            smartAttendanceProcessor.processRfidEvent(request);
        } catch (Exception e) {
            log.error("Error procesando asistencia: {}", e.getMessage());
        }
    }
}