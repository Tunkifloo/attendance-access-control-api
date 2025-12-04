package com.iot.attendance.infrastructure.config;

import com.iot.attendance.infrastructure.persistence.entity.RfidCardEntity;
import com.iot.attendance.infrastructure.persistence.repository.RfidCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RfidInitializerService {

    private final RfidCardRepository rfidCardRepository;
    private static final List<String> SYSTEM_RFID_UIDS = Arrays.asList(
            "3513B5B1",
            "85DB6DB1",
            "BA910FB1",
            "40C86F61",
            "FD5FC801"
    );

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeRfidCards() {
        log.info("=".repeat(60));
        log.info("INICIALIZANDO TARJETAS RFID DEL SISTEMA");
        log.info("=".repeat(60));

        int created = 0;
        int existing = 0;

        for (String uid : SYSTEM_RFID_UIDS) {
            if (rfidCardRepository.findById(uid).isEmpty()) {
                RfidCardEntity card = RfidCardEntity.builder()
                        .uid(uid)
                        .worker(null)
                        .lastSeen(null)
                        .build();

                rfidCardRepository.save(card);
                log.info("✓ Tarjeta RFID creada: {}", uid);
                created++;
            } else {
                log.info("○ Tarjeta RFID ya existe: {}", uid);
                existing++;
            }
        }

        log.info("=".repeat(60));
        log.info("RESUMEN: {} creadas | {} existentes | {} total",
                created, existing, SYSTEM_RFID_UIDS.size());
        log.info("=".repeat(60));
    }

    public void logRfidStatus() {
        log.info("\n--- ESTADO DE TARJETAS RFID ---");

        List<RfidCardEntity> allCards = rfidCardRepository.findAll();

        for (RfidCardEntity card : allCards) {
            String status = card.getWorker() != null
                    ? "ASIGNADA a Worker ID: " + card.getWorker().getId()
                    : "DISPONIBLE (sin asignar)";

            log.info("RFID: {} | Estado: {} | Último uso: {}",
                    card.getUid(),
                    status,
                    card.getLastSeen() != null ? card.getLastSeen() : "Nunca");
        }

        log.info("-------------------------------\n");
    }
}