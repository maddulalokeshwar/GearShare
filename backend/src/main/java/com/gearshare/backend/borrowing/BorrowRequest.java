
package com.gearshare.backend.borrowing;

import com.gearshare.backend.gear.Gear;
import com.gearshare.backend.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "borrow_requests")
public class BorrowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gear_id", nullable = false)
    @JsonIgnore
    private Gear gear;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    @JsonIgnore
    private User requester;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowRequestStatus status = BorrowRequestStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BorrowRequest() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = BorrowRequestStatus.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public Gear getGear() {
        return gear;
    }

    public void setGear(Gear gear) {
        this.gear = gear;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BorrowRequestStatus getStatus() {
        return status;
    }

    public void setStatus(BorrowRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}