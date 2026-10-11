
package com.gearshare.backend.borrowing;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BorrowRequestResponse(
        Long id,
        Long gearId,
        String gearName,
        Long ownerId,
        Long requesterId,
        String requesterName,
        BorrowRequestStatus status,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt
) {
    public static BorrowRequestResponse from(
            BorrowRequest request) {

        return new BorrowRequestResponse(
                request.getId(),
                request.getGear().getId(),
                request.getGear().getName(),
                request.getGear().getOwnerId(),
                request.getRequester().getId(),
                request.getRequester().getName(),
                request.getStatus(),
                request.getStartDate(),
                request.getEndDate(),
                request.getCreatedAt()
        );
    }
}
