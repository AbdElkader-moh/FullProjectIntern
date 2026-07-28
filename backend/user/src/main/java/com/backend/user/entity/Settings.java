package com.backend.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@Entity
@Table(name = "settings")
public class Settings {

    @Id
    private String id;
    private Long userId;
    private String type;
    private String metric;
    private Float thresholdValue;
    private String alertType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.systemDefault());

    public Settings() {}

    public Settings(Long userId, String type, String metric, Float thresholdValue, String alertType) {
        this.id = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.type = type;
        this.metric = metric;
        this.thresholdValue = thresholdValue;
        this.alertType = alertType;
    }
}
