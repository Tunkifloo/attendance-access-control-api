package com.iot.attendance.infrastructure.persistence.repository;

import com.iot.attendance.domain.enums.AccessStatus;
import com.iot.attendance.infrastructure.persistence.entity.AccessLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLogEntity, Long> {

    List<AccessLogEntity> findByWorkerIdOrderByAccessTimeDesc(Long workerId);

    List<AccessLogEntity> findByAccessTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<AccessLogEntity> findByStatus(AccessStatus status);

    @Query("SELECT a FROM AccessLogEntity a WHERE a.workerId = :workerId AND a.accessTime BETWEEN :startTime AND :endTime")
    List<AccessLogEntity> findByWorkerIdAndTimeRange(
            @Param("workerId") Long workerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT a FROM AccessLogEntity a WHERE a.status = 'DENIED' AND a.accessTime > :since ORDER BY a.accessTime DESC")
    List<AccessLogEntity> findRecentDeniedAccess(@Param("since") LocalDateTime since);
}