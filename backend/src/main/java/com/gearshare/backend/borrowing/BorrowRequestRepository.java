
package com.gearshare.backend.borrowing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface BorrowRequestRepository
        extends JpaRepository<BorrowRequest, Long> {

    @EntityGraph(attributePaths = {
            "gear",
            "requester",
            "gear.owner"
    })
    List<BorrowRequest> findByRequester_IdOrderByCreatedAtDesc(
            Long requesterId
    );

    @EntityGraph(attributePaths = {
            "gear",
            "requester",
            "gear.owner"
    })
    List<BorrowRequest> findByGear_Owner_IdOrderByCreatedAtDesc(
            Long ownerId
    );

    boolean existsByGear_IdAndRequester_IdAndStatus(
            Long gearId,
            Long requesterId,
            BorrowRequestStatus status
    );

    @EntityGraph(attributePaths = {
            "gear",
            "requester",
            "gear.owner"
    })
    Optional<BorrowRequest> findWithDetailsById(Long id);

    @Query("""
        SELECT COUNT(r) > 0
        FROM BorrowRequest r
        WHERE r.gear.id = :gearId
          AND r.status = :status
          AND r.startDate <= :endDate
          AND r.endDate >= :startDate
        """)
    boolean existsOverlappingBooking(
            @Param("gearId") Long gearId,
            @Param("status") BorrowRequestStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @EntityGraph(attributePaths = {
            "gear",
            "requester",
            "gear.owner"
    })
    @Query("""
        SELECT r
        FROM BorrowRequest r
        WHERE r.gear.id = :gearId
          AND r.status = :status
          AND r.endDate >= :today
        ORDER BY r.startDate ASC
        """)
    List<BorrowRequest> findUpcomingBookings(
            @Param("gearId") Long gearId,
            @Param("status") BorrowRequestStatus status,
            @Param("today") LocalDate today
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM BorrowRequest r WHERE r.id = :id")
    Optional<BorrowRequest> findByIdForUpdate(
            @Param("id") Long id
    );
}