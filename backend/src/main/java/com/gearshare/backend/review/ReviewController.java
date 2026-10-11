package com.gearshare.backend.review;

import com.gearshare.backend.borrowing.BorrowRequest;
import com.gearshare.backend.borrowing.BorrowRequestRepository;
import com.gearshare.backend.borrowing.BorrowRequestStatus;
import com.gearshare.backend.user.User;
import com.gearshare.backend.user.UserRepository;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final UserRepository userRepository;

    public ReviewController(
            ReviewRepository reviewRepository,
            BorrowRequestRepository borrowRequestRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.borrowRequestRepository = borrowRequestRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please sign in first");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User account not found"));
    }

    @PostMapping("/{borrowRequestId}")
    @Transactional
    public ReviewResponse createReview(
            @PathVariable Long borrowRequestId,
            @Valid @RequestBody ReviewRequest body,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        if (body == null || body.rating() == null
                || body.rating() < 1 || body.rating() > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        BorrowRequest request = borrowRequestRepository
                .findWithDetailsById(borrowRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Borrow request not found"));

        if (request.getStatus() != BorrowRequestStatus.RETURNED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You can review a rental only after it has been returned");
        }

        User requester = request.getRequester();
        User owner = request.getGear() == null
                ? null : request.getGear().getOwner();

        boolean isRequester = requester != null
                && requester.getId().equals(user.getId());
        boolean isOwner = owner != null
                && owner.getId().equals(user.getId());

        if (!isRequester && !isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the borrower or gear owner can review this rental");
        }

        if (reviewRepository.existsByBorrowRequest_IdAndReviewer_Id(
                borrowRequestId, user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already reviewed this rental");
        }

        Review review = new Review();
        review.setBorrowRequest(request);
        review.setReviewer(user);
        review.setRating(body.rating());
        review.setComment(
                body.comment() == null ? null : body.comment().trim());

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @GetMapping("/gear/{gearId}")
    @Transactional(readOnly = true)
    public List<ReviewResponse> getGearReviews(@PathVariable Long gearId) {
        return reviewRepository
                .findByBorrowRequest_Gear_IdOrderByCreatedAtDesc(gearId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @GetMapping("/request/{borrowRequestId}")
    @Transactional(readOnly = true)
    public List<ReviewResponse> getRequestReviews(
            @PathVariable Long borrowRequestId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        BorrowRequest request = borrowRequestRepository
                .findWithDetailsById(borrowRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Borrow request not found"));

        User requester = request.getRequester();
        User owner = request.getGear() == null
                ? null : request.getGear().getOwner();

        boolean isRequester = requester != null
                && requester.getId().equals(user.getId());
        boolean isOwner = owner != null
                && owner.getId().equals(user.getId());

        if (!isRequester && !isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot view reviews for this rental");
        }

        return reviewRepository
                .findByBorrowRequest_IdOrderByCreatedAtDesc(borrowRequestId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }
}