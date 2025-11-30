package com.iot.attendance.infrastructure.firebase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.attendance.application.dto.request.RfidAttendanceRequest;
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
    private static final Pattern RFID_PATTERN = Pattern.compile("Marcaje RFID: ([A-F0-9 ]+)");
    private final Set<String> processedKeys = Collections.synchronizedSet(new HashSet<>());

    @Scheduled(fixedRate = 3000)
    public void pollRecentAttendance() {
        try {
            // Consultar los últimos 5 registros vía REST
            // limitToLast=5 es suficiente para el ritmo de un control de asistencia
            String url = String.format("%s/logs/asistencia.json?orderBy=\"$key\"&limitToLast=5", databaseUrl);

            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.equals("null")) return;

            // Parsear JSON (Firebase devuelve un Map: {"key1": "valor1", "key2": "valor2"})
            JsonNode rootNode = objectMapper.readTree(response);
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                String message = entry.getValue().asText();

                // Procesar solo si no lo hemos visto antes
                if (!processedKeys.contains(key)) {
                    processLogMessage(message);
                    processedKeys.add(key);

                    // Limpieza simple de memoria: si el set crece mucho (ej. > 1000), limpiarlo
                    if (processedKeys.size() > 1000) processedKeys.clear();
                }
            }

        } catch (Exception e) {
            log.error("Error en polling de asistencia: {}", e.getMessage());
        }
    }

    private void processLogMessage(String message) {
        Matcher matcher = RFID_PATTERN.matcher(message);
        if (matcher.find()) {
            String rawUid = matcher.group(1);
            String normalizedUid = rawUid.toUpperCase().replace(" ", "").trim();

            log.info(">> [REST POLLING] RFID Detectado: {}", normalizedUid);

            // LÓGICA DE CAPTURA
            Optional<RfidCardEntity> cardOpt = rfidCardRepository.findById(normalizedUid);

            if (cardOpt.isEmpty()) {
                // NUEVA TARJETA -> Guardar en Pool
                log.info("   -> Tarjeta NUEVA. Guardando en BD...");
                RfidCardEntity newCard = RfidCardEntity.builder()
                        .uid(normalizedUid)
                        .worker(null)
                        .lastSeen(LocalDateTime.now())
                        .build();
                rfidCardRepository.save(newCard);
            } else {
                // TARJETA EXISTENTE
                RfidCardEntity card = cardOpt.get();
                card.setLastSeen(LocalDateTime.now());
                rfidCardRepository.save(card);

                if (card.getWorker() != null) {
                    // TIENE DUEÑO -> Procesar Asistencia
                    processCheckInCheckOut(normalizedUid);
                }
            }
        }
    }

    private void processCheckInCheckOut(String rfidUid) {
        try {
            RfidAttendanceRequest request = RfidAttendanceRequest.builder()
                    .rfidUid(rfidUid)
                    .timestamp(LocalDateTime.now())
                    .build();
            smartAttendanceProcessor.processRfidEvent(request);
        } catch (Exception e) {
            log.error("Error procesando asistencia smart: {}", e.getMessage());
        }
    }
}