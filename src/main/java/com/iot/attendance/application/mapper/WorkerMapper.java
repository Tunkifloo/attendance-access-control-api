package com.iot.attendance.application.mapper;

import com.iot.attendance.domain.model.Worker;
import com.iot.attendance.domain.valueobjects.FingerprintId;
import com.iot.attendance.domain.valueobjects.RfidTag;
import com.iot.attendance.infrastructure.persistence.entity.WorkerEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class WorkerMapper {

    public Worker toDomain(WorkerEntity entity) {
        return Worker.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .documentNumber(entity.getDocumentNumber())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .fingerprintId(entity.getFingerprintId() != null ?
                        FingerprintId.of(entity.getFingerprintId()) : null)
                .rfidTags(entity.getRfidTags().stream()
                        .map(RfidTag::of)
                        .collect(Collectors.toSet()))
                .hasRestrictedAreaAccess(entity.isHasRestrictedAreaAccess())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public WorkerEntity toEntity(Worker domain) {
        return WorkerEntity.builder()
                .id(domain.getId())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .documentNumber(domain.getDocumentNumber())
                .email(domain.getEmail())
                .phoneNumber(domain.getPhoneNumber())
                .fingerprintId(domain.getFingerprintId() != null ?
                        domain.getFingerprintId().getValue() : null)
                .rfidTags(domain.getRfidTags().stream()
                        .map(RfidTag::getUid)
                        .collect(Collectors.toSet()))
                .hasRestrictedAreaAccess(domain.isHasRestrictedAreaAccess())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
