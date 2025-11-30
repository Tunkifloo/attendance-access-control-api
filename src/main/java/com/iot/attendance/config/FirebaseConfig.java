package com.iot.attendance.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.database-url}")
    private String databaseUrl;

    @Value("${firebase.credentials-path}")
    private String credentialsPath;

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        log.info("Initializing Firebase with credentials from: {}", credentialsPath);

        try {
            // Intentar cargar desde classpath (resources)
            ClassPathResource resource = new ClassPathResource(credentialsPath);

            if (!resource.exists()) {
                log.error("Firebase credentials file not found at: {}", credentialsPath);
                throw new IOException("Firebase credentials file not found: " + credentialsPath);
            }

            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(databaseUrl)
                    .setConnectTimeout(30000)
                    .setReadTimeout(30000)
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);

            log.info("Firebase initialized successfully!");
            log.info("Firebase Database URL: {}", databaseUrl);
            log.info("Firebase App Name: {}", app.getName());

            return app;

        } catch (IOException e) {
            log.error("Failed to initialize Firebase. Error: {}", e.getMessage());
            log.error("Make sure the file '{}' exists in src/main/resources/", credentialsPath);
            log.error("Full error details: ", e);
            throw e;
        }
    }

    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp firebaseApp) {
        log.info("Creating Firebase Realtime Database instance");
        FirebaseDatabase database = FirebaseDatabase.getInstance(firebaseApp);
        database.setPersistenceEnabled(false);
        log.info("Firebase Database instance created successfully");
        return database;
    }
}