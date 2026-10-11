package com.gearshare.backend.review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long borrowRequestId,
        Long gearId,
        String gearName,
        Long reviewerId,
        String reviewerName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        var request = review.getBorrowRequest();
        var gear = request.getGear();
        var reviewer = review.getReviewer();

        return new ReviewResponse(
                review.getId(),
                request.getId(),
                gear.getId(),
                gear.getName(),
                reviewer.getId(),
                reviewer.getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}