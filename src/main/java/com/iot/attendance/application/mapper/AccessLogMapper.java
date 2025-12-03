package com.iot.attendance.application.mapper;

import com.iot.attendance.domain.model.AccessLog;
import com.iot.attendance.infrastructure.persistence.entity.AccessLogEntity;
import org.springframework.stereotype.Component;

@Component
public class AccessLogMapper {

    public AccessLog toDomain(AccessLogEntity entity) {
        return AccessLog.builder()
                .id(entity.getId())
                .workerId(entity.getWorkerId())
                .fingerprintId(entity.getFingerprintId())
                .accessGranted(entity.isAccessGranted())
                .location(entity.getLocation())
                .accessTime(entity.getAccessTime())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public AccessLogEntity toEntity(AccessLog domain) {
        return AccessLogEntity.builder()
                .id(domain.getId())
                .workerId(domain.getWorkerId())
                .fingerprintId(domain.getFingerprintId())
                .accessGranted(domain.isAccessGranted())
                .location(domain.getLocation())
                .accessTime(domain.getAccessTime())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}