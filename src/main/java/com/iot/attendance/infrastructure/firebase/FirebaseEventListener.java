package com.iot.attendance.infrastructure.firebase;

import com.google.firebase.database.*;
import com.iot.attendance.application.service.WorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseEventListener {

    private final FirebaseDatabase firebaseDatabase;
    private final FirebaseRealtimeService firebaseService;
    private final WorkerService workerService;

    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        log.info("Starting Firebase event listeners...");

        listenToAdminCommands();
    }

    private void listenToAdminCommands() {
        DatabaseReference ref = firebaseDatabase.getReference("admin/comando");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String command = snapshot.getValue(String.class);

                    if (command != null && !command.equals("NADA")) {
                        log.info("Received admin command from Firebase: {}", command);
                        processAdminCommand(command);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Error listening to admin commands: {}", error.getMessage());
            }
        });

        log.info("Firebase admin command listener started");
    }

    private void processAdminCommand(String command) {
        try {
            switch (command) {
                case "REGISTRAR":
                    log.info("Processing REGISTRAR command - waiting for ESP32 to handle");
                    firebaseService.setAdminState("REGISTRO_EN_PROCESO");
                    break;

                case "BORRAR":
                    log.info("Processing BORRAR command - waiting for ESP32 to handle");
                    firebaseService.setAdminState("BORRADO_EN_PROCESO");
                    break;

                case "FORMATEAR":
                    log.info("Processing FORMATEAR command - waiting for ESP32 to handle");
                    firebaseService.setAdminState("FORMATEO_EN_PROCESO");
                    break;

                default:
                    log.debug("Unknown command: {}", command);
                    break;
            }

        } catch (Exception e) {
            log.error("Error processing admin command {}: {}", command, e.getMessage());
            firebaseService.setAdminCommand("NADA");
        }
    }
}