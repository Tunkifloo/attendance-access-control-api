package com.iot.attendance.application.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "Hora de inicio de jornada", example = "08:00:00")
    private LocalTime workStartTime;

    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "Hora de fin de jornada", example = "17:00:00")
    private LocalTime workEndTime;

    @Min(value = 0, message = "Late threshold must be non-negative")
    @Schema(description = "Minutos de tolerancia para tardanzas", example = "15")
    private Integer lateThresholdMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha simulada para testing", example = "2024-01-15")
    private LocalDate simulatedDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fecha/hora simulada completa", example = "2024-01-15T08:30:00")
    private LocalDateTime simulatedDateTime;

    @Schema(description = "Activar modo simulación", example = "true")
    private Boolean simulationMode;
}