package com.gearshare.backend.borrowing;

import com.gearshare.backend.gear.Gear;
import com.gearshare.backend.gear.GearRepository;
import com.gearshare.backend.notification.NotificationService;
import com.gearshare.backend.user.User;
import com.gearshare.backend.user.UserRepository;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/borrow-requests")
public class BorrowRequestController {

    private final BorrowRequestRepository requestRepository;
    private final GearRepository gearRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public BorrowRequestController(
            BorrowRequestRepository requestRepository,
            GearRepository gearRepository,
            UserRepository userRepository,
            NotificationService notificationService) {

        this.requestRepository = requestRepository;
        this.gearRepository = gearRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please sign in first"
            );
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User account not found"
                ));
    }

    private BorrowRequest getRequest(Long id) {
        return requestRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Borrow request not found"
                ));
    }

    private void verifyOwner(BorrowRequest request, User user) {
        if (request.getGear() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The gear listing associated with this request no longer exists"
            );
        }

        Long ownerId = request.getGear().getOwnerId();

        if (ownerId == null || !ownerId.equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the gear owner can perform this action"
            );
        }
    }

    private void requirePending(BorrowRequest request) {
        if (request.getStatus() != BorrowRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending requests can be approved or rejected"
            );
        }
    }

    private void validateDates(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both start date and return date are required"
            );
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The start date cannot be in the past"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The return date must be on or after the start date"
            );
        }
    }

    private boolean hasOverlappingApprovedBooking(
            Long gearId,
            LocalDate startDate,
            LocalDate endDate) {

        return requestRepository.existsOverlappingBooking(
                gearId,
                BorrowRequestStatus.APPROVED,
                startDate,
                endDate
        );
    }

    /*
     * Finds the earliest date on which a booking of the same
     * requested duration could fit, based on current approved
     * bookings.
     */
    private LocalDate findNextAvailableDate(
            Long gearId,
            LocalDate requestedStart,
            LocalDate requestedEnd) {

        long durationDays = ChronoUnit.DAYS.between(
                requestedStart,
                requestedEnd
        );

        List<BorrowRequest> bookings =
                requestRepository.findUpcomingBookings(
                        gearId,
                        BorrowRequestStatus.APPROVED,
                        requestedStart
                );

        LocalDate candidateStart = requestedStart;

        for (BorrowRequest booking : bookings) {
            LocalDate bookedStart = booking.getStartDate();
            LocalDate bookedEnd = booking.getEndDate();

            if (bookedStart == null || bookedEnd == null) {
                continue;
            }

            LocalDate candidateEnd =
                    candidateStart.plusDays(durationDays);

            boolean overlaps =
                    !bookedStart.isAfter(candidateEnd) &&
                    !bookedEnd.isBefore(candidateStart);

            if (overlaps) {
                candidateStart = bookedEnd.plusDays(1);
            }
        }

        return candidateStart;
    }

    @GetMapping("/{gearId}/availability")
    @Transactional(readOnly = true)
    public GearAvailabilityResponse checkAvailability(
            @PathVariable Long gearId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        validateDates(startDate, endDate);

        gearRepository.findById(gearId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Gear listing not found"
                ));

        boolean available = !hasOverlappingApprovedBooking(
                gearId,
                startDate,
                endDate
        );

        LocalDate nextAvailableDate = null;

        if (!available) {
            nextAvailableDate = findNextAvailableDate(
                    gearId,
                    startDate,
                    endDate
            );
        }

        return new GearAvailabilityResponse(
                gearId,
                startDate,
                endDate,
                available,
                available ? "AVAILABLE" : "BOOKED",
                nextAvailableDate
        );
    }

    @PostMapping("/{gearId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public BorrowRequestResponse createRequest(
            @PathVariable Long gearId,
            @RequestBody BorrowRequestDateRequest bookingDates,
            Authentication authentication) {

        User requester = getCurrentUser(authentication);

        if (bookingDates == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking dates are required"
            );
        }

        LocalDate startDate = bookingDates.startDate();
        LocalDate endDate = bookingDates.endDate();

        validateDates(startDate, endDate);

        // Preserve the gear lock before checking booking availability.
        Gear gear = gearRepository.findByIdForUpdate(gearId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Gear listing not found"
                ));

        Long ownerId = gear.getOwnerId();

        if (ownerId == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This listing has no assigned owner and cannot receive requests"
            );
        }

        if (ownerId.equals(requester.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot request your own gear"
            );
        }

        if (hasOverlappingApprovedBooking(
                gearId,
                startDate,
                endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This gear is already booked for some or all of those dates"
            );
        }

        BorrowRequest request = new BorrowRequest();
        request.setGear(gear);
        request.setRequester(requester);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setStatus(BorrowRequestStatus.PENDING);

        BorrowRequest savedRequest = requestRepository.save(request);

        // Notify the gear owner about the new request.
        User owner = gear.getOwner();

        if (owner != null) {
            notificationService.create(
                    owner,
                    requester.getName()
                            + " requested to borrow "
                            + gear.getName()
                            + " from "
                            + startDate
                            + " to "
                            + endDate
                            + ".",
                    "BORROW_REQUEST",
                    savedRequest.getId()
            );
        }

        return BorrowRequestResponse.from(savedRequest);
    }

    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public List<BorrowRequestResponse> myRequests(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return requestRepository
                .findByRequester_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(BorrowRequestResponse::from)
                .toList();
    }

    @GetMapping("/incoming")
    @Transactional(readOnly = true)
    public List<BorrowRequestResponse> incomingRequests(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return requestRepository
                .findByGear_Owner_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(BorrowRequestResponse::from)
                .toList();
    }

    @GetMapping("/{id}/contact-details")
    @Transactional(readOnly = true)
    public BorrowContactDetailsResponse contactDetails(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        BorrowRequest request = getRequest(id);

        if (request.getStatus() != BorrowRequestStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Contact details are available only for approved requests"
            );
        }

        if (request.getGear() == null ||
                request.getRequester() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The gear or requester information is unavailable"
            );
        }

        User owner = request.getGear().getOwner();
        User requester = request.getRequester();

        if (owner == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The gear owner information is unavailable"
            );
        }

        boolean isOwner =
                owner.getId().equals(currentUser.getId());

        boolean isRequester =
                requester.getId().equals(currentUser.getId());

        if (!isOwner && !isRequester) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the gear owner and approved borrower can view these details"
            );
        }

        User contact = isOwner ? requester : owner;

        return new BorrowContactDetailsResponse(
                request.getId(),
                request.getGear().getName(),
                contact.getName(),
                contact.getEmail(),
                contact.getPhoneNumber(),
                request.getGear().getPickupAddress()
        );
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public BorrowRequestResponse approveRequest(
            @PathVariable Long id,
            Authentication authentication) {

        User owner = getCurrentUser(authentication);
        BorrowRequest request = getRequest(id);

        verifyOwner(request, owner);
        requirePending(request);

        if (request.getStartDate() == null ||
                request.getEndDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This request has no booking dates. Ask the borrower to create a new dated request."
            );
        }

        // Preserve the shared gear lock before the final availability check.
        Gear lockedGear = gearRepository.findByIdForUpdate(
                request.getGear().getId()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Gear listing not found"
        ));

        validateDates(
                request.getStartDate(),
                request.getEndDate()
        );

        if (hasOverlappingApprovedBooking(
                lockedGear.getId(),
                request.getStartDate(),
                request.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot approve this request because those dates are already booked"
            );
        }

        request.setStatus(BorrowRequestStatus.APPROVED);

        BorrowRequest savedRequest = requestRepository.save(request);

        // Notify the borrower that the request was approved.
        notificationService.create(
                request.getRequester(),
                "Your request to borrow "
                        + lockedGear.getName()
                        + " has been approved.",
                "REQUEST_APPROVED",
                savedRequest.getId()
        );

        return BorrowRequestResponse.from(savedRequest);
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public BorrowRequestResponse rejectRequest(
            @PathVariable Long id,
            Authentication authentication) {

        User owner = getCurrentUser(authentication);
        BorrowRequest request = getRequest(id);

        verifyOwner(request, owner);
        requirePending(request);

        request.setStatus(BorrowRequestStatus.REJECTED);

        BorrowRequest savedRequest = requestRepository.save(request);

        // Notify the borrower that the request was rejected.
        notificationService.create(
                request.getRequester(),
                "Your request to borrow "
                        + request.getGear().getName()
                        + " was declined.",
                "REQUEST_REJECTED",
                savedRequest.getId()
        );

        return BorrowRequestResponse.from(savedRequest);
    }

    @PostMapping("/{id}/return")
    @Transactional
    public BorrowRequestResponse returnRequest(
            @PathVariable Long id,
            Authentication authentication) {

        User owner = getCurrentUser(authentication);
        BorrowRequest request = getRequest(id);

        verifyOwner(request, owner);

        if (request.getStatus() != BorrowRequestStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only approved requests can be marked as returned"
            );
        }

        request.setStatus(BorrowRequestStatus.RETURNED);

        BorrowRequest savedRequest = requestRepository.save(request);

        // Notify the borrower that the return has been recorded.
        notificationService.create(
                request.getRequester(),
                "The return of "
                        + request.getGear().getName()
                        + " has been recorded.",
                "GEAR_RETURNED",
                savedRequest.getId()
        );

        return BorrowRequestResponse.from(savedRequest);
    }
}