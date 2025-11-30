package com.iot.attendance.infrastructure.persistence.repository;

import com.iot.attendance.domain.enums.AttendanceStatus;
import com.iot.attendance.infrastructure.persistence.entity.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

    Optional<AttendanceEntity> findByWorkerIdAndAttendanceDateAndStatus(
            Long workerId,
            LocalDate attendanceDate,
            AttendanceStatus status
    );

    List<AttendanceEntity> findByWorkerIdAndAttendanceDateBetween(
            Long workerId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<AttendanceEntity> findByAttendanceDate(LocalDate attendanceDate);

    @Query("SELECT a FROM AttendanceEntity a WHERE a.attendanceDate = :date AND a.isLate = true")
    List<AttendanceEntity> findLateAttendancesByDate(@Param("date") LocalDate date);

    @Query("SELECT a FROM AttendanceEntity a WHERE a.workerId = :workerId AND a.status = 'CHECKED_IN'")
    Optional<AttendanceEntity> findActiveAttendanceByWorkerId(@Param("workerId") Long workerId);

    @Query("SELECT COUNT(a) FROM AttendanceEntity a WHERE a.workerId = :workerId AND a.attendanceDate BETWEEN :startDate AND :endDate AND a.isLate = true")
    long countLateAttendances(
            @Param("workerId") Long workerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}