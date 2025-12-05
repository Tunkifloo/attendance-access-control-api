package com.iot.attendance.infrastructure.persistence.repository;

import com.iot.attendance.infrastructure.persistence.entity.AccessLogEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLogEntity, Long> {

    List<AccessLogEntity> findByWorkerIdOrderByAccessTimeDesc(Long workerId);

    List<AccessLogEntity> findByAccessTimeBetween(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Sort sort
    );

    // Nuevo método para filtrar por estado y rango de tiempo con ordenamiento
    List<AccessLogEntity> findByAccessTimeBetweenAndStatus(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status,
            Sort sort
    );

    @Query("SELECT a FROM AccessLogEntity a WHERE a.accessGranted = false " +
            "AND a.accessTime > :since ORDER BY a.accessTime DESC")
    List<AccessLogEntity> findRecentDeniedAccess(@Param("since") LocalDateTime since);

    @Query("SELECT a FROM AccessLogEntity a WHERE a.accessGranted = true " +
            "AND a.accessTime > :since ORDER BY a.accessTime DESC")
    List<AccessLogEntity> findRecentGrantedAccess(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AccessLogEntity a WHERE a.workerId = :workerId " +
            "AND a.accessGranted = false " +
            "AND a.accessTime BETWEEN :startTime AND :endTime")
    long countDeniedAccessesByWorker(
            @Param("workerId") Long workerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}