package com.iot.attendance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.iot.attendance.infrastructure.persistence.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class PostgresConfig {

    // PostgreSQL 17 aprovecha mejores índices BRIN y BTREE
    // La configuración principal está en application.yml
}