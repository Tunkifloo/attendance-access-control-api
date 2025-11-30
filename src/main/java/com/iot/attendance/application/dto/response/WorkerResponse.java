package com.iot.attendance.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iot.attendance.domain.enums.WorkerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkerResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String documentNumber;
    private String email;
    private String phoneNumber;
    private Integer fingerprintId;
    private Set<String> rfidTags;
    private boolean hasRestrictedAreaAccess;
    private WorkerStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}