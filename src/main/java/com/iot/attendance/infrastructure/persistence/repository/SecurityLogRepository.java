package com.iot.attendance.infrastructure.persistence.repository;

import com.iot.attendance.infrastructure.persistence.entity.SecurityLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLogEntity, Long> {

    List<SecurityLogEntity> findByEventTimeBetweenOrderByEventTimeDesc(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    List<SecurityLogEntity> findBySeverityOrderByEventTimeDesc(String severity);

    @Query("SELECT s FROM SecurityLogEntity s WHERE s.eventType = :eventType AND s.eventTime > :since")
    List<SecurityLogEntity> findRecentEventsByType(
            @Param("eventType") String eventType,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT s FROM SecurityLogEntity s WHERE s.severity IN ('HIGH', 'CRITICAL') ORDER BY s.eventTime DESC")
    List<SecurityLogEntity> findCriticalEvents();
}