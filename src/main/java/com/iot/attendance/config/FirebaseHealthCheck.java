package com.iot.attendance.config;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.iot.attendance.infrastructure.firebase.FirebaseRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseHealthCheck {
/*
    private final FirebaseDatabase firebaseDatabase;
    private final FirebaseRealtimeService firebaseService;

    @EventListener(ApplicationReadyEvent.class)
    public void verifyFirebaseConnection() {
        log.info("=".repeat(80));
        log.info("FIREBASE HEALTH CHECK - DIAGNÓSTICO COMPLETO");
        log.info("=".repeat(80));

        try {
            // 1. Verificar referencia base
            DatabaseReference rootRef = firebaseDatabase.getReference();
            String dbUrl = rootRef.toString();
            log.info("✓ Database URL: {}", dbUrl);
            log.info("✓ Root Reference: {}", rootRef.getPath());

            // 2. Verificar referencia específica
            DatabaseReference adminRef = firebaseDatabase.getReference("admin/comando");
            log.info("✓ Admin Reference: {}", adminRef.toString());

            // 3. Generar token de prueba
            String testToken = "TEST_" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Token de prueba: {}", testToken);

            // 4. PRUEBA DE ESCRITURA
            log.info(">> Fase 1: ESCRIBIENDO a Firebase...");
            firebaseService.setAdminCommand(testToken);
            log.info("✓ Comando enviado (sin errores locales)");

            // Esperar propagación
            log.info("Esperando 3 segundos para propagación...");
            Thread.sleep(3000);

            // 5. PRUEBA DE LECTURA
            log.info(">> Fase 2: LEYENDO desde Firebase...");
            String currentValue = firebaseService.getAdminCommandSync();
            log.info("✓ Valor recuperado: '{}'", currentValue);

            // 6. VALIDACIÓN
            log.info(">> Fase 3: VALIDACIÓN");
            if (testToken.equals(currentValue)) {
                log.info("¡ÉXITO! Los valores coinciden perfectamente.");
                log.info("La conexión a Firebase está 100% operativa.");
            } else {
                log.error("ERROR DE VALIDACIÓN:");
                log.error("   Esperado: '{}'", testToken);
                log.error("   Recibido: '{}'", currentValue);
                log.error("");
                log.error("DIAGNÓSTICO:");

                if (currentValue == null || "NADA".equals(currentValue)) {
                    log.error("   → PROBLEMA DE LECTURA detectado");
                    log.error("   → Verifica las reglas de seguridad de Firebase:");
                    log.error("      {");
                    log.error("        \"rules\": {");
                    log.error("          \".read\": \"auth != null\",");
                    log.error("          \".write\": \"auth != null\"");
                    log.error("        }");
                    log.error("      }");
                    log.error("");
                    log.error("   → Para desarrollo, puedes usar temporalmente:");
                    log.error("      {");
                    log.error("        \"rules\": {");
                    log.error("          \".read\": true,");
                    log.error("          \".write\": true");
                    log.error("        }");
                    log.error("      }");
                    log.error("");
                    log.error("   → Verifica permisos en firebase-credentials.json");
                    log.error("   → URL correcta: {}", dbUrl);
                } else {
                    log.error("   → PROBLEMA DE SINCRONIZACIÓN/LATENCIA");
                    log.error("   → La escritura podría estar fallando silenciosamente");
                }
            }

        } catch (Exception e) {
        log.error(" FALLO CRÍTICO EN HEALTH CHECK");
        log.error("Tipo de error: {}", e.getClass().getSimpleName());
        log.error("Mensaje: {}", e.getMessage());
        log.error("", e);

        log.error("");
        log.error(" ACCIONES RECOMENDADAS:");
        log.error("  1. Verifica firebase-credentials.json en src/main/resources/");
        log.error("  2. Verifica URL de Firebase en application.yml");
        log.error("  3. Revisa reglas de seguridad en Firebase Console");
        log.error("  4. Verifica conectividad de red (firewall/proxy)");
        log.error("  5. Confirma que el Service Account tiene rol 'Firebase Admin SDK Administrator Service Agent'");

    } finally {
        // Leer todo el nodo admin
        try {
            log.info("");
            log.info(">> Ejecutando diagnóstico adicional...");
            firebaseService.diagnoseAdminNode();
            Thread.sleep(2000); // Esperar a que se complete
        } catch (Exception ex) {
            log.warn("No se pudo ejecutar diagnóstico: {}", ex.getMessage());
        }

        // LIMPIEZA
        try {
            firebaseService.setAdminCommand("NADA");
            log.info(" Limpieza completada (Comando reseteado a NADA)");
        } catch (Exception ex) {
            log.warn(" No se pudo limpiar el comando de prueba: {}", ex.getMessage());
        }
    }
        log.info("=".repeat(80));
   }*/
}
