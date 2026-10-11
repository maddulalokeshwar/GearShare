
package com.gearshare.backend.notification;

import com.gearshare.backend.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository
    ) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void create(
            User recipient,
            String message,
            String type,
            Long relatedRequestId
    ) {
        if (recipient == null || recipient.getId() == null) {
            throw new IllegalArgumentException(
                    "A valid notification recipient is required"
            );
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRelatedRequestId(relatedRequestId);

        notificationRepository.save(notification);
    }
}
