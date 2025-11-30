package com.iot.attendance.infrastructure.persistence.repository;

import com.iot.attendance.domain.enums.WorkerStatus;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<WorkerEntity, Long> {

    Optional<WorkerEntity> findByDocumentNumber(String documentNumber);

    Optional<WorkerEntity> findByFingerprintId(Integer fingerprintId);

    @Query("SELECT w FROM WorkerEntity w JOIN w.rfidTags r WHERE r = :rfidTag")
    Optional<WorkerEntity> findByRfidTag(@Param("rfidTag") String rfidTag);

    List<WorkerEntity> findByStatus(WorkerStatus status);

    @Query("SELECT w FROM WorkerEntity w WHERE w.hasRestrictedAreaAccess = true AND w.status = 'ACTIVE'")
    List<WorkerEntity> findActiveWorkersWithRestrictedAccess();

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByFingerprintId(Integer fingerprintId);

    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM WorkerEntity w JOIN w.rfidTags r WHERE r = :rfidTag")
    boolean existsByRfidTag(@Param("rfidTag") String rfidTag);
}