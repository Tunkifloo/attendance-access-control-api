package com.iot.attendance.infrastructure.firebase;

import com.google.firebase.database.*;
import com.iot.attendance.infrastructure.exception.FirebaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseRealtimeService {

    private final FirebaseDatabase firebaseDatabase;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void logAttendance(String rfidUid, LocalDateTime timestamp, boolean isLate) {
        try {
            DatabaseReference ref = firebaseDatabase.getReference("asistencia");

            String message = String.format("Marcaje RFID: %s", rfidUid);

            ref.push().setValue(message, (error, ref1) -> {
                if (error != null) {
                    log.error("Error writing to Firebase: {}", error.getMessage());
                } else {
                    log.info("Attendance logged to Firebase for RFID: {}", rfidUid);
                }
            });

        } catch (Exception e) {
            log.error("Error logging attendance to Firebase: {}", e.getMessage());
            throw new FirebaseException("Failed to log attendance to Firebase", e);
        }
    }

    public void logAccessGranted(Integer fingerprintId, Long workerId) {
        try {
            DatabaseReference ref = firebaseDatabase.getReference("accesos");

            String message = String.format("Puerta abierta ID: %d", fingerprintId);

            ref.push().setValue(message, (error, ref1) -> {
                if (error != null) {
                    log.error("Error writing access to Firebase: {}", error.getMessage());
                } else {
                    log.info("Access granted logged to Firebase for fingerprint: {}", fingerprintId);
                }
            });

        } catch (Exception e) {
            log.error("Error logging access to Firebase: {}", e.getMessage());
            throw new FirebaseException("Failed to log access to Firebase", e);
        }
    }

    public void logAccessDenied(Integer fingerprintId, String reason) {
        try {
            DatabaseReference ref = firebaseDatabase.getReference("seguridad");

            String message = String.format("Intento fallido huella: %d - %s", fingerprintId, reason);

            ref.push().setValue(message, (error, ref1) -> {
                if (error != null) {
                    log.error("Error writing denied access to Firebase: {}", error.getMessage());
                } else {
                    log.info("Access denied logged to Firebase for fingerprint: {}", fingerprintId);
                }
            });

        } catch (Exception e) {
            log.error("Error logging denied access to Firebase: {}", e.getMessage());
            throw new FirebaseException("Failed to log denied access to Firebase", e);
        }
    }

    public CompletableFuture<String> getAdminCommand() {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            DatabaseReference ref = firebaseDatabase.getReference("admin/comando");

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String command = snapshot.getValue(String.class);
                        future.complete(command != null ? command : "NADA");
                    } else {
                        future.complete("NADA");
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    log.error("Error getting admin command: {}", error.getMessage());
                    future.completeExceptionally(
                            new FirebaseException("Failed to get admin command: " + error.getMessage())
                    );
                }
            });

        } catch (Exception e) {
            log.error("Error getting admin command from Firebase: {}", e.getMessage());
            future.completeExceptionally(new FirebaseException("Failed to get admin command", e));
        }

        return future;
    }

    public void setAdminCommand(String command) {
        try {
            DatabaseReference ref = firebaseDatabase.getReference("admin/comando");

            ref.setValue(command, (error, ref1) -> {
                if (error != null) {
                    log.error("Error setting admin command: {}", error.getMessage());
                } else {
                    log.info("Admin command set to: {}", command);
                }
            });

        } catch (Exception e) {
            log.error("Error setting admin command: {}", e.getMessage());
            throw new FirebaseException("Failed to set admin command", e);
        }
    }

    public void setAdminState(String state) {
        try {
            DatabaseReference ref = firebaseDatabase.getReference("admin/estado");

            ref.setValue(state, (error, ref1) -> {
                if (error != null) {
                    log.error("Error setting admin state: {}", error.getMessage());
                } else {
                    log.info("Admin state set to: {}", state);
                }
            });

        } catch (Exception e) {
            log.error("Error setting admin state: {}", e.getMessage());
            throw new FirebaseException("Failed to set admin state", e);
        }
    }

    public void setLastFingerprintId(Integer fingerprintId) {
        try {
            DatabaseReference ref = firebaseDatabase.getReference("admin/ultimo_id_creado");

            ref.setValue(fingerprintId, (error, ref1) -> {
                if (error != null) {
                    log.error("Error setting last fingerprint ID: {}", error.getMessage());
                } else {
                    log.info("Last fingerprint ID set to: {}", fingerprintId);
                }
            });

        } catch (Exception e) {
            log.error("Error setting last fingerprint ID: {}", e.getMessage());
            throw new FirebaseException("Failed to set last fingerprint ID", e);
        }
    }

    public void setTargetFingerprintId(Integer fingerprintId) {
        try {
            DatabaseReference ref = firebaseDatabase.getReference("admin/id_target");

            ref.setValue(fingerprintId, (error, ref1) -> {
                if (error != null) {
                    log.error("Error setting target fingerprint ID: {}", error.getMessage());
                } else {
                    log.info("Target fingerprint ID set to: {}", fingerprintId);
                }
            });

        } catch (Exception e) {
            log.error("Error setting target fingerprint ID: {}", e.getMessage());
            throw new FirebaseException("Failed to set target fingerprint ID", e);
        }
    }

    public String getAdminCommandSync() {
        CountDownLatch latch = new CountDownLatch(1);
        final String[] result = {null};

        DatabaseReference ref = firebaseDatabase.getReference("admin/comando");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    result[0] = snapshot.getValue(String.class);
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Error getting admin command sync: {}", error.getMessage());
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirebaseException("Timeout waiting for Firebase response", e);
        }

        return result[0] != null ? result[0] : "NADA";
    }

    public void listenToAdminCommands(AdminCommandListener listener) {
        DatabaseReference ref = firebaseDatabase.getReference("admin/comando");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String command = snapshot.getValue(String.class);
                    if (command != null && !command.equals("NADA")) {
                        log.info("Admin command received: {}", command);
                        listener.onCommandReceived(command);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Error listening to admin commands: {}", error.getMessage());
                listener.onError(error.getMessage());
            }
        });
    }

    @FunctionalInterface
    public interface AdminCommandListener {
        void onCommandReceived(String command);

        default void onError(String error) {
            log.error("Admin command listener error: {}", error);
        }
    }
}