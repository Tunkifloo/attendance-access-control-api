package com.iot.attendanceaccesscontrolapi.domain.model;

import com.iot.attendanceaccesscontrolapi.domain.enums.AttendanceStatus;
import com.iot.attendanceaccesscontrolapi.domain.valueobjects.RfidTag;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {

    private Long id;

    private Long workerId;

    private RfidTag rfidTag;

    private LocalDate attendanceDate;

    @Setter
    private LocalDateTime checkInTime;

    @Setter
    private LocalDateTime checkOutTime;

    @Setter
    private Duration workedDuration;

    @Setter
    private boolean isLate;

    @Setter
    private Duration latenessDuration;

    @Setter
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.CHECKED_IN;

    private LocalDateTime createdAt;

    @Setter
    private LocalDateTime updatedAt;

    public void checkOut(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
        this.status = AttendanceStatus.CHECKED_OUT;
        this.workedDuration = Duration.between(checkInTime, checkOutTime);
        this.updatedAt = LocalDateTime.now();
    }

    public void calculateLateness(LocalTime workStartTime) {
        LocalTime checkInOnlyTime = checkInTime.toLocalTime();
        if (checkInOnlyTime.isAfter(workStartTime)) {
            this.isLate = true;
            this.latenessDuration = Duration.between(
                    workStartTime.atDate(attendanceDate),
                    checkInTime
            );
        } else {
            this.isLate = false;
            this.latenessDuration = Duration.ZERO;
        }
    }

    public boolean isCheckedOut() {
        return this.status == AttendanceStatus.CHECKED_OUT;
    }
}