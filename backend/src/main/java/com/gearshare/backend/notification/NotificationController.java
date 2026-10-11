
package com.gearshare.backend.notification;

import com.gearshare.backend.user.User;
import com.gearshare.backend.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    private User currentUser(Authentication authentication) {
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

    @GetMapping
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(
            Authentication authentication
    ) {
        User user = currentUser(authentication);

        return notificationRepository
                .findByRecipient_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @GetMapping("/unread-count")
    @Transactional(readOnly = true)
    public long getUnreadCount(Authentication authentication) {
        User user = currentUser(authentication);

        return notificationRepository
                .countByRecipient_IdAndReadFalse(user.getId());
    }

    @PostMapping("/{id}/read")
    @Transactional
    public NotificationResponse markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = currentUser(authentication);

        Notification notification = notificationRepository
                .findByIdAndRecipient_Id(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found"
                ));

        notification.setRead(true);

        return NotificationResponse.from(
                notificationRepository.save(notification)
        );
    }

    @PostMapping("/read-all")
    @Transactional
    public int markAllAsRead(Authentication authentication) {
        User user = currentUser(authentication);

        List<Notification> notifications =
                notificationRepository
                        .findByRecipient_IdOrderByCreatedAtDesc(
                                user.getId()
                        );

        int updated = 0;

        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                notification.setRead(true);
                updated++;
            }
        }

        notificationRepository.saveAll(notifications);

        return updated;
    }
}
