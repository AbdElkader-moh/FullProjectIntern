package com.backend.sensor_data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseSensorData {

    @Id
    protected String id;

    @NotBlank
    protected String location;

    @Column(updatable = false)
    protected LocalDateTime timestamp = LocalDateTime.now(ZoneId.of("UTC"));

    protected BaseSensorData() {
        this.id = java.util.UUID.randomUUID().toString();
    }
}
