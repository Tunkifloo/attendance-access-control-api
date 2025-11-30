package com.iot.attendance.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateWorkerRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    private String lastName;

    @NotBlank(message = "Document number is required")
    @Pattern(regexp = "^[0-9]{8}$", message = "Document number must be 8 digits")
    private String documentNumber;

    @Email(message = "Email must be valid")
    private String email;

    @Pattern(regexp = "^[0-9]{9}$", message = "Phone number must be 9 digits")
    private String phoneNumber;

    private Set<String> rfidTags;

    @Builder.Default
    private boolean hasRestrictedAreaAccess = false;
}