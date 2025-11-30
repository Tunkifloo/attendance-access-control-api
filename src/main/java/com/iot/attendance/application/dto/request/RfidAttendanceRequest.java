package com.iot.attendance.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RfidAttendanceRequest {

    @NotBlank(message = "RFID UID is required")
    private String rfidUid;

    private LocalDateTime timestamp;
}