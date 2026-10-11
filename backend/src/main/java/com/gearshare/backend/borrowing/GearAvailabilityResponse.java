
package com.gearshare.backend.borrowing;

import java.time.LocalDate;

public record GearAvailabilityResponse(
        Long gearId,
        LocalDate startDate,
        LocalDate endDate,
        boolean available,
        String status,
        LocalDate nextAvailableDate
) {}
