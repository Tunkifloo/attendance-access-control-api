package com.iot.attendance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI attendanceAccessControlOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:" + serverPort);
        devServer.setDescription("Development Server");

        Contact contact = new Contact();
        contact.setName("IoT Attendance Team");
        contact.setEmail("support@attendance-iot.com");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Attendance & Access Control API")
                .version("1.0.0")
                .contact(contact)
                .description("Enterprise-grade REST API for IoT-based attendance and access control system. " +
                        "Implements biometric authentication via fingerprint sensors and RFID-based attendance tracking.")
                .license(license)
                .termsOfService("https://attendance-iot.com/terms");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer));
    }
}