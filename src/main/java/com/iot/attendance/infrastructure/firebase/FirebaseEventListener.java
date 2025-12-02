package com.iot.attendance.infrastructure.firebase;

import com.google.firebase.database.*;
import com.iot.attendance.application.dto.request.RfidAttendanceRequest;
import com.iot.attendance.application.dto.request.FingerprintAccessRequest;
import com.iot.attendance.application.service.impl.SmartAttendanceProcessor;
import com.iot.attendance.application.service.AccessControlService;
import com.iot.attendance.infrastructure.persistence.entity.RfidCardEntity;
import com.iot.attendance.infrastructure.persistence.repository.RfidCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseEventListener {

    private final FirebaseDatabase firebaseDatabase;
    private final SmartAttendanceProcessor smartAttendanceProcessor;
    private final AccessControlService accessControlService;
    private final RfidCardRepository rfidCardRepository;

    private static final Pattern RFID_PATTERN = Pattern.compile("Marcaje RFID: ([A-F0-9 ]+)");
    private static final Pattern ACCESS_PATTERN = Pattern.compile("Puerta abierta ID: (\\d+)");

    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        log.info("Starting Firebase Real-time Listeners...");
        listenToAttendance();
        listenToAccessLogs();
        log.info("All Firebase listeners are active");
    }

    private void listenToAttendance() {
        DatabaseReference ref = firebaseDatabase.getReference("logs/asistencia");

        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                processAttendanceEvent(snapshot);
            }
            @Override public void onChildChanged(DataSnapshot s, String p) {}
            @Override public void onChildRemoved(DataSnapshot s) {}
            @Override public void onChildMoved(DataSnapshot s, String p) {}
            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Error listening to attendance: {}", error.getMessage());
            }
        });
        log.info("✓ Listener Asistencia: Capturando RFIDs para el pool.");
    }

    private void processAttendanceEvent(DataSnapshot snapshot) {
        try {
            String message = snapshot.getValue(String.class);
            if (message == null) return;

            Matcher matcher = RFID_PATTERN.matcher(message);
            if (matcher.find()) {
                // Extraer y Normalizar (Quitar espacios: "40 C8 6F 61" -> "40C86F61")
                String rawUid = matcher.group(1);
                String normalizedUid = rawUid.toUpperCase().replace(" ", "").trim();

                // Lógica de "UPSERT" (Insertar o Actualizar)
                Optional<RfidCardEntity> cardOpt = rfidCardRepository.findById(normalizedUid);

                if (cardOpt.isEmpty()) {
                    // --- CASO NUEVO: Guardar en el Pool ---
                    log.info(">> Nueva tarjeta detectada: {}. Guardando en BD...", normalizedUid);

                    RfidCardEntity newCard = RfidCardEntity.builder()
                            .uid(normalizedUid)
                            .worker(null)
                            .lastSeen(LocalDateTime.now())
                            .build();

                    rfidCardRepository.save(newCard);
                    log.info("✓ Tarjeta {} guardada en rfid_cards.", normalizedUid);

                } else {
                    // --- CASO EXISTENTE: Actualizar 'lastSeen' ---
                    RfidCardEntity card = cardOpt.get();
                    card.setLastSeen(LocalDateTime.now());
                    rfidCardRepository.save(card);

                    // Si ya tiene dueño, procesar asistencia
                    if (card.getWorker() != null) {
                        processCheckInCheckOut(normalizedUid);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error procesando evento RFID: {}", e.getMessage());
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
            log.error("Error procesando asistencia: {}", e.getMessage());
        }
    }

    private void listenToAccessLogs() {
        DatabaseReference ref = firebaseDatabase.getReference("logs/accesos");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                processAccessEvent(snapshot);
            }
            @Override public void onChildChanged(DataSnapshot s, String p) {}
            @Override public void onChildRemoved(DataSnapshot s) {}
            @Override public void onChildMoved(DataSnapshot s, String p) {}
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void processAccessEvent(DataSnapshot snapshot) {
        try {
            String message = snapshot.getValue(String.class);
            if (message == null) return;

            Matcher matcher = ACCESS_PATTERN.matcher(message);
            if (matcher.find()) {
                Integer fingerprintId = Integer.parseInt(matcher.group(1));

                FingerprintAccessRequest request = FingerprintAccessRequest.builder()
                        .fingerprintId(fingerprintId)
                        .location("Área Restringida")
                        .timestamp(LocalDateTime.now())
                        .build();

                accessControlService.processAccess(request);
            }
        } catch (Exception e) {
            log.error("Error procesando acceso: {}", e.getMessage());
        }
    }
}