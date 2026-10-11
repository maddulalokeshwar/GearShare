
package com.gearshare.backend.user;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String role,
        LocalDateTime createdAt
) {
}
