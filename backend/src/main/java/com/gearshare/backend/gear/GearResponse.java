
package com.gearshare.backend.gear;

import java.math.BigDecimal;

public record GearResponse(
        Long id,
        String name,
        String description,
        String category,
        BigDecimal pricePerDay,
        Long ownerId
) {
    public static GearResponse from(Gear gear) {
        return new GearResponse(
                gear.getId(),
                gear.getName(),
                gear.getDescription(),
                gear.getCategory(),
                gear.getPricePerDay(),
                gear.getOwnerId()
        );
    }
}
