package com.iot.attendance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class AttendanceAccessControlApiApplication {

    public static void main(String[] args) throws UnknownHostException {
        SpringApplication app = new SpringApplication(AttendanceAccessControlApiApplication.class);
        Environment env = app.run(args).getEnvironment();

        String protocol = "http";
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path", "");

        log.info("\n----------------------------------------------------------\n" +
                        "Application '{}' is running! Access URLs:\n" +
                        "Local: \t\t{}://localhost:{}{}\n" +
                        "External: \t{}://{}:{}{}\n" +
                        "Swagger UI: \t{}://localhost:{}{}/swagger-ui.html\n" +
                        "Profile(s): \t{}\n" +
                        "----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                protocol, port, contextPath,
                protocol, hostAddress, port, contextPath,
                protocol, port, contextPath,
                env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles()
        );
    }
}
