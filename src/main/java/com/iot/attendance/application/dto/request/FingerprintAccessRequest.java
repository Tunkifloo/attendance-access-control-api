package com.iot.attendance.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FingerprintAccessRequest {

    @NotNull(message = "Fingerprint ID is required")
    @Positive(message = "Fingerprint ID must be positive")
    private Integer fingerprintId;

    private String location;

    private LocalDateTime timestamp;
}