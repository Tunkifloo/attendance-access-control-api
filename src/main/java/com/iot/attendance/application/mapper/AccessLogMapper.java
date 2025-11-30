package com.iot.attendance.application.mapper;

import com.iot.attendance.domain.model.AccessLog;
import com.iot.attendance.domain.valueobjects.FingerprintId;
import com.iot.attendance.infrastructure.persistence.entity.AccessLogEntity;
import org.springframework.stereotype.Component;

@Component
public class AccessLogMapper {

    public AccessLog toDomain(AccessLogEntity entity) {
        return AccessLog.builder()
                .id(entity.getId())
                .workerId(entity.getWorkerId())
                .fingerprintId(entity.getFingerprintId() != null ?
                        FingerprintId.of(entity.getFingerprintId()) : null)
                .status(entity.getStatus())
                .location(entity.getLocation())
                .denialReason(entity.getDenialReason())
                .accessTime(entity.getAccessTime())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public AccessLogEntity toEntity(AccessLog domain) {
        return AccessLogEntity.builder()
                .id(domain.getId())
                .workerId(domain.getWorkerId())
                .fingerprintId(domain.getFingerprintId() != null ?
                        domain.getFingerprintId().getValue() : null)
                .status(domain.getStatus())
                .location(domain.getLocation())
                .denialReason(domain.getDenialReason())
                .accessTime(domain.getAccessTime())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
