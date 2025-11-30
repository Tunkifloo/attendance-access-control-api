package com.iot.attendance.infrastructure.persistence.repository;

import com.iot.attendance.infrastructure.persistence.entity.SystemConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfigurationEntity, Long> {

    @Query("SELECT s FROM SystemConfigurationEntity s ORDER BY s.id DESC LIMIT 1")
    Optional<SystemConfigurationEntity> findLatestConfiguration();
}