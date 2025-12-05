package com.iot.attendance.infrastructure.firebase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.attendance.application.dto.request.RfidAttendanceRequest;
import com.iot.attendance.application.service.AccessAuditService;
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

    private static final Pattern RFID_PATTERN = Pattern.compile("Marcaje RFID: ([A-F0-9 ]+)");
    private static final Pattern ACCESS_GRANTED_PATTERN = Pattern.compile("Puerta abierta ID: (\\d+)");
    private static final Pattern ACCESS_DENIED_PATTERN = Pattern.compile("Intento fallido huella|Huella desconocida");

    private final Set<String> processedAttendanceKeys = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> processedAccessKeys = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> processedSecurityKeys = Collections.synchronizedSet(new HashSet<>());

    @Scheduled(fixedRate = 3000)
    public void pollRecentAttendance() {
        pollGeneric("asistencia", processedAttendanceKeys, this::processAttendanceMessage);
    }

    @Scheduled(fixedRate = 3000)
    public void pollAccessLogs() {
        pollGeneric("accesos", processedAccessKeys, this::processAccessGrantedMessage);
    }

    @Scheduled(fixedRate = 3000)
    public void pollSecurityLogs() {
        pollGeneric("seguridad", processedSecurityKeys, this::processAccessDeniedMessage);
    }

    private void pollGeneric(String node, Set<String> processedKeys, java.util.function.Consumer<String> processor) {
        try {
            String url = String.format("%s/logs/%s.json?orderBy=\"$key\"&limitToLast=5", databaseUrl, node);
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.equals("null")) return;
            JsonNode rootNode = objectMapper.readTree(response);
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                if (!processedKeys.contains(key)) {
                    processor.accept(entry.getValue().asText());
                    processedKeys.add(key);
                    if (processedKeys.size() > 1000) processedKeys.clear();
                }
            }
        } catch (Exception e) { log.error("Error polling {}: {}", node, e.getMessage()); }
    }

    private void processAttendanceMessage(String message) {
        Matcher matcher = RFID_PATTERN.matcher(message);
        if (matcher.find()) {
            String rawUid = matcher.group(1).toUpperCase().replace(" ", "").trim();
            Optional<RfidCardEntity> cardOpt = rfidCardRepository.findById(rawUid);
            if (cardOpt.isEmpty()) {
                log.warn("⚠ RFID NO REGISTRADO: {}", rawUid);
                return;
            }
            RfidCardEntity card = cardOpt.get();
            card.setLastSeen(LocalDateTime.now());
            rfidCardRepository.save(card);
            if (card.getWorker() != null) processCheckInCheckOut(rawUid);
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
            log.info(">> [ACCESO DENEGADO DETECTADO]");
            accessAuditService.logAccessDenied(null, LocalDateTime.now());
        }
    }

    private void processCheckInCheckOut(String rfidUid) {
        try {
            RfidAttendanceRequest request = RfidAttendanceRequest.builder()
                    .rfidUid(rfidUid).timestamp(LocalDateTime.now()).build();
            smartAttendanceProcessor.processRfidEvent(request);
        } catch (Exception e) { log.error("Error procesando asistencia: {}", e.getMessage()); }
    }
}