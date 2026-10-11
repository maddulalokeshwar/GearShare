package com.gearshare.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name cannot exceed 100 characters")
        String name,

        @Size(max = 30, message = "Phone number cannot exceed 30 characters")
        String phoneNumber
) {
}