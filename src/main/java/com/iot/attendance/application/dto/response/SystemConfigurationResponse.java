package com.iot.attendance.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemConfigurationResponse {

    private Long id;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime workStartTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime workEndTime;

    private Integer lateThresholdMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate currentAttendanceDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime simulatedDateTime;

    private boolean simulationMode;

    private Integer maxFailedAccessAttempts;

    private Integer alertCooldownMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}