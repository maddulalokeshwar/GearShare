
package com.gearshare.backend.borrowing;

public record BorrowContactDetailsResponse(
        Long requestId,
        String gearName,
        String contactName,
        String contactEmail,
        String contactPhoneNumber,
        String pickupAddress
) {
}
