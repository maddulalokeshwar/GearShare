
package com.gearshare.backend.notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String message,
        String type,
        Long relatedRequestId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.getRelatedRequestId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
