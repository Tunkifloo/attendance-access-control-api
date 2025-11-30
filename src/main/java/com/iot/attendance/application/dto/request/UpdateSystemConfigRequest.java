package com.iot.attendance.application.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
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
public class UpdateSystemConfigRequest {

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime workStartTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime workEndTime;

    @Min(value = 0, message = "Late threshold must be non-negative")
    private Integer lateThresholdMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate currentAttendanceDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime simulatedDateTime;

    private Boolean simulationMode;

    @Min(value = 1, message = "Max failed attempts must be at least 1")
    private Integer maxFailedAccessAttempts;

    @Min(value = 1, message = "Alert cooldown must be at least 1 minute")
    private Integer alertCooldownMinutes;
}