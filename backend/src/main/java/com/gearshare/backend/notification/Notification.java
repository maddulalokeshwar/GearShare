
package com.gearshare.backend.notification;

import com.gearshare.backend.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(name = "related_request_id")
    private Long relatedRequestId;

    @Column(name = "is_read", nullable = false)
private boolean read = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Notification() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getRelatedRequestId() {
        return relatedRequestId;
    }

    public void setRelatedRequestId(Long relatedRequestId) {
        this.relatedRequestId = relatedRequestId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
