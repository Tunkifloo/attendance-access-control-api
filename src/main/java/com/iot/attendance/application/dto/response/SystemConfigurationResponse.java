package com.iot.attendance.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "ID de configuración", example = "1")
    private Long id;

    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "Hora de inicio de jornada", example = "08:00:00")
    private LocalTime workStartTime;

    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "Hora de fin de jornada", example = "17:00:00")
    private LocalTime workEndTime;

    @Schema(description = "Minutos de tolerancia para tardanzas", example = "15")
    private Integer lateThresholdMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha simulada (si está activa)", example = "2024-01-15")
    private LocalDate simulatedDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fecha/hora simulada completa", example = "2024-01-15T08:30:00")
    private LocalDateTime simulatedDateTime;

    @Schema(description = "Indica si el modo simulación está activo", example = "false")
    private boolean simulationMode;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}