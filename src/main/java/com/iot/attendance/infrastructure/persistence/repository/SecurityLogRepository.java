package com.iot.attendance.infrastructure.persistence.repository;

import com.iot.attendance.infrastructure.persistence.entity.SecurityLogEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLogEntity, Long> {

    List<SecurityLogEntity> findByEventTimeBetween(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Sort sort
    );

    List<SecurityLogEntity> findByEventTimeBetweenAndSeverity(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String severity,
            Sort sort
    );
}