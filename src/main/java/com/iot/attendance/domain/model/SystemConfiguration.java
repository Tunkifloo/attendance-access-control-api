package com.iot.attendance.domain.model;

import lombok.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemConfiguration {

    private Long id;

    @Setter
    private LocalTime workStartTime;

    @Setter
    private LocalTime workEndTime;

    @Setter
    private Integer lateThresholdMinutes;

    @Setter
    private LocalDate simulatedDate;

    @Setter
    private LocalDateTime simulatedDateTime;

    @Setter
    private boolean simulationMode;

    private LocalDateTime createdAt;

    @Setter
    private LocalDateTime updatedAt;

    public LocalDateTime getCurrentDateTime() {
        if (simulationMode && simulatedDateTime != null) {
            return simulatedDateTime;
        }
        return LocalDateTime.now();
    }

    public LocalDate getCurrentDate() {
        if (simulationMode) {
            if (simulatedDate != null) {
                return simulatedDate;
            }
            if (simulatedDateTime != null) {
                return simulatedDateTime.toLocalDate();
            }
        }
        return LocalDate.now();
    }

    public LocalDateTime getLateThresholdTime(LocalDate date) {
        LocalDateTime workStartDateTime = date.atTime(workStartTime);
        return workStartDateTime.plusMinutes(lateThresholdMinutes != null ? lateThresholdMinutes : 0);
    }

    public boolean isLateCheckIn(LocalDateTime checkInTime) {
        LocalDateTime threshold = getLateThresholdTime(checkInTime.toLocalDate());
        return checkInTime.isAfter(threshold);
    }

    public Duration calculateLatenessDuration(LocalDateTime checkInTime) {
        if (!isLateCheckIn(checkInTime)) {
            return Duration.ZERO;
        }

        LocalDateTime workStartDateTime = checkInTime.toLocalDate().atTime(workStartTime);
        return Duration.between(workStartDateTime, checkInTime);
    }

    public void validate() {
        if (workStartTime == null) {
            throw new IllegalStateException("Work start time cannot be null");
        }
        if (workEndTime == null) {
            throw new IllegalStateException("Work end time cannot be null");
        }
        if (workStartTime.isAfter(workEndTime)) {
            throw new IllegalStateException("Work start time must be before work end time");
        }
        if (lateThresholdMinutes != null && lateThresholdMinutes < 0) {
            throw new IllegalStateException("Late threshold minutes cannot be negative");
        }
    }
}