package com.example.demo.entity;

import com.example.demo.util.LocalINDTime;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "rental_order_events")
public class RentalOrderEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, length = 20)
    private String orderId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    @Column(name = "actor", length = 100)
    private String actor;

    @Column(name = "actor_role", length = 40)
    private String actorRole;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "user_visible", nullable = false)
    private boolean userVisible;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;



    @PrePersist
    void onCreate() {
        LocalDateTime n = LocalINDTime.now();
        if (createdAt == null) createdAt = n;
    }

    public UUID getId() { return id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }
    public String getEventType() { return eventType; }
    public void setEventType(String v) { this.eventType = v; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String v) { this.fromStatus = v; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String v) { this.toStatus = v; }
    public String getActor() { return actor; }
    public void setActor(String v) { this.actor = v; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String v) { this.actorRole = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public boolean isUserVisible() { return userVisible; }
    public void setUserVisible(boolean v) { this.userVisible = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
