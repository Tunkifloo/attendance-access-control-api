package com.iot.attendance.infrastructure.persistence.repository;

import com.iot.attendance.infrastructure.persistence.entity.RfidCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RfidCardRepository extends JpaRepository<RfidCardEntity, String> {
    List<RfidCardEntity> findByWorkerIsNull();
}