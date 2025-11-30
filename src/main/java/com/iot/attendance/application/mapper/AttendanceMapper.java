package com.iot.attendance.application.mapper;

import com.iot.attendance.domain.model.Attendance;
import com.iot.attendance.domain.valueobjects.RfidTag;
import com.iot.attendance.infrastructure.persistence.entity.AttendanceEntity;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public Attendance toDomain(AttendanceEntity entity) {
        return Attendance.builder()
                .id(entity.getId())
                .workerId(entity.getWorkerId())
                .rfidTag(RfidTag.of(entity.getRfidTag()))
                .attendanceDate(entity.getAttendanceDate())
                .checkInTime(entity.getCheckInTime())
                .checkOutTime(entity.getCheckOutTime())
                .workedDuration(entity.getWorkedDuration())
                .isLate(entity.isLate())
                .latenessDuration(entity.getLatenessDuration())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public AttendanceEntity toEntity(Attendance domain) {
        AttendanceEntity entity = AttendanceEntity.builder()
                .id(domain.getId())
                .workerId(domain.getWorkerId())
                .rfidTag(domain.getRfidTag().getUid())
                .attendanceDate(domain.getAttendanceDate())
                .checkInTime(domain.getCheckInTime())
                .checkOutTime(domain.getCheckOutTime())
                .isLate(domain.isLate())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();

        entity.setWorkedDuration(domain.getWorkedDuration());
        entity.setLatenessDuration(domain.getLatenessDuration());

        return entity;
    }
}
