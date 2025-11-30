package com.iot.attendance.domain.model;

import lombok.*;

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
    private LocalDate currentAttendanceDate;

    @Setter
    private LocalDateTime simulatedDateTime;

    @Setter
    private boolean simulationMode;

    @Setter
    private Integer maxFailedAccessAttempts;

    @Setter
    private Integer alertCooldownMinutes;

    private LocalDateTime createdAt;

    @Setter
    private LocalDateTime updatedAt;

    public LocalDateTime getCurrentDateTime() {
        return simulationMode && simulatedDateTime != null
                ? simulatedDateTime
                : LocalDateTime.now();
    }
}