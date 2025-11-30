package com.iot.attendance.config;

import com.google.firebase.database.FirebaseDatabase;
import com.iot.attendance.infrastructure.firebase.FirebaseRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseHealthCheck {

    private final FirebaseDatabase firebaseDatabase;
    private final FirebaseRealtimeService firebaseService;

    @EventListener(ApplicationReadyEvent.class)
    public void verifyFirebaseConnection() {
        log.info("=".repeat(80));
        log.info("FIREBASE HEALTH CHECK");
        log.info("=".repeat(80));

        try {
            // Verificar que la conexión funciona
            String dbUrl = firebaseDatabase.getReference().toString();
            log.info("✓ Firebase Database Reference: {}", dbUrl);

            // Intentar escribir un valor de prueba
            firebaseService.setAdminCommand("HEALTH_CHECK");
            log.info("✓ Firebase write test: SUCCESS");

            // Leer el valor
            String command = firebaseService.getAdminCommandSync();
            log.info("✓ Firebase read test: SUCCESS (Value: {})", command);

            // Restaurar valor
            firebaseService.setAdminCommand("NADA");
            log.info("✓ Firebase connection is HEALTHY");

        } catch (Exception e) {
            log.error("✗ Firebase connection FAILED: {}", e.getMessage());
            log.error("Please check your firebase-credentials.json file");
        }

        log.info("=".repeat(80));
    }
}