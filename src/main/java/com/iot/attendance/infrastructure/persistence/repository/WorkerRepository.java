package com.iot.attendance.infrastructure.persistence.repository;

import com.iot.attendance.domain.enums.WorkerStatus;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<WorkerEntity, Long> {

    Optional<WorkerEntity> findByDocumentNumber(String documentNumber);

    Optional<WorkerEntity> findByFingerprintId(Integer fingerprintId);

    List<WorkerEntity> findByStatus(WorkerStatus status);

    @Query("SELECT w FROM WorkerEntity w WHERE w.hasRestrictedAreaAccess = true AND w.status = 'ACTIVE'")
    List<WorkerEntity> findActiveWorkersWithRestrictedAccess();

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByFingerprintId(Integer fingerprintId);
}