package com.iot.attendance.infrastructure.firebase;

import com.google.firebase.database.*;
import com.iot.attendance.application.dto.request.RfidAttendanceRequest;
import com.iot.attendance.application.dto.request.FingerprintAccessRequest;
import com.iot.attendance.application.service.AttendanceService;
import com.iot.attendance.application.service.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseEventListener {

    private final FirebaseDatabase firebaseDatabase;
    private final AttendanceService attendanceService;
    private final AccessControlService accessControlService;

    // Patrón para extraer RFID del mensaje: "Marcaje RFID: 40 C8 6F 61"
    private static final Pattern RFID_PATTERN = Pattern.compile("Marcaje RFID: ([A-F0-9 ]+)");

    // Patrón para extraer ID de huella: "Puerta abierta ID: 1"
    private static final Pattern ACCESS_PATTERN = Pattern.compile("Puerta abierta ID: (\\d+)");

    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        log.info("==========================================");
        log.info("Starting Firebase Real-time Listeners...");
        log.info("==========================================");

        listenToAttendance();
        listenToAccessLogs();
        listenToAdminCommands();

        log.info("All Firebase listeners are active");
    }

    private void listenToAttendance() {
        DatabaseReference ref = firebaseDatabase.getReference("asistencia");

        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                processAttendanceEvent(snapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                // No necesario para este caso
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                // No necesario para este caso
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                // No necesario para este caso
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Error listening to attendance: {}", error.getMessage());
            }
        });

        log.info("✓ Attendance listener registered on /asistencia");
    }

    private void processAttendanceEvent(DataSnapshot snapshot) {
        try {
            String message = snapshot.getValue(String.class);
            if (message == null) return;

            log.info("New attendance event from ESP32: {}", message);

            // Extraer el UID RFID del mensaje
            Matcher matcher = RFID_PATTERN.matcher(message);
            if (matcher.find()) {
                String rfidUid = matcher.group(1);
                log.info("Extracted RFID UID: {}", rfidUid);

                // Crear request y procesar asistencia
                RfidAttendanceRequest request = RfidAttendanceRequest.builder()
                        .rfidUid(rfidUid)
                        .timestamp(LocalDateTime.now())
                        .build();

                // Determinar si es check-in o check-out
                // Por defecto, asumimos que es check-in si no hay asistencia activa
                try {
                    attendanceService.recordCheckIn(request);
                    log.info("✓ Check-in processed successfully for RFID: {}", rfidUid);
                } catch (Exception e) {
                    // Si falla porque ya hay check-in activo, intentar check-out
                    try {
                        attendanceService.recordCheckOut(request);
                        log.info("✓ Check-out processed successfully for RFID: {}", rfidUid);
                    } catch (Exception ex) {
                        log.error("Failed to process attendance: {}", ex.getMessage());
                    }
                }
            } else {
                log.warn("Could not extract RFID from message: {}", message);
            }

        } catch (Exception e) {
            log.error("Error processing attendance event: {}", e.getMessage(), e);
        }
    }

    private void listenToAccessLogs() {
        DatabaseReference ref = firebaseDatabase.getReference("accesos");

        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                processAccessEvent(snapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Error listening to access: {}", error.getMessage());
            }
        });

        log.info("✓ Access listener registered on /accesos");
    }

    private void listenToFingerprintRegistration() {
        DatabaseReference ref = firebaseDatabase.getReference("admin/ultimo_id_creado");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer fingerprintId = snapshot.getValue(Integer.class);
                    log.info("ESP32 registered new fingerprint ID: {}", fingerprintId);

                    // Aquí podríamos actualizar automáticamente el worker
                    // o notificar al frontend
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Error: {}", error.getMessage());
            }
        });
    }

    private void processAccessEvent(DataSnapshot snapshot) {
        try {
            String message = snapshot.getValue(String.class);
            if (message == null) return;

            log.info("New access event from ESP32: {}", message);

            // Extraer el ID de huella del mensaje: "Puerta abierta ID: 1"
            Matcher matcher = ACCESS_PATTERN.matcher(message);
            if (matcher.find()) {
                Integer fingerprintId = Integer.parseInt(matcher.group(1));
                log.info("Extracted fingerprint ID: {}", fingerprintId);

                // Crear request y procesar acceso
                FingerprintAccessRequest request = FingerprintAccessRequest.builder()
                        .fingerprintId(fingerprintId)
                        .location("Área Restringida")
                        .timestamp(LocalDateTime.now())
                        .build();

                accessControlService.processAccess(request);
                log.info("✓ Access processed successfully for fingerprint: {}", fingerprintId);
            } else {
                log.warn("Could not extract fingerprint ID from message: {}", message);
            }

        } catch (Exception e) {
            log.error("Error processing access event: {}", e.getMessage(), e);
        }
    }

    private void listenToAdminCommands() {
        DatabaseReference ref = firebaseDatabase.getReference("admin/comando");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String command = snapshot.getValue(String.class);

                    if (command != null && !command.equals("NADA")) {
                        log.info("Admin command detected: {}", command);
                        // Los comandos son manejados por el ESP32
                        // La API solo los monitorea para logging
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Error listening to admin commands: {}", error.getMessage());
            }
        });

        log.info("✓ Admin command listener registered on /admin/comando");
    }
}