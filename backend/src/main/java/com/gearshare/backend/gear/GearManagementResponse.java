
package com.gearshare.backend.gear;

import java.math.BigDecimal;

public record GearManagementResponse(
        Long id,
        String name,
        String description,
        String category,
        BigDecimal pricePerDay,
        Long ownerId,
        String pickupAddress,
        Double latitude,
        Double longitude
) {
    public static GearManagementResponse from(Gear gear) {
        return new GearManagementResponse(
                gear.getId(),
                gear.getName(),
                gear.getDescription(),
                gear.getCategory(),
                gear.getPricePerDay(),
                gear.getOwnerId(),
                gear.getPickupAddress(),
                gear.getLatitude(),
                gear.getLongitude()
        );
    }
}
