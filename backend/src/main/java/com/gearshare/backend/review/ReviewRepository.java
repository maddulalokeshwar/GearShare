package com.gearshare.backend.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBorrowRequest_IdAndReviewer_Id(
        Long borrowRequestId,
        Long reviewerId
    );

    List<Review> findByBorrowRequest_Gear_IdOrderByCreatedAtDesc(
        Long gearId
    );

    List<Review> findByBorrowRequest_IdOrderByCreatedAtDesc(
        Long borrowRequestId
    );
}
