package com.backend.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    @Column(nullable = false, unique = true)
    @Setter
    private String email;

    @Column(name = "first_name", nullable = false)
    @Setter
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @Setter
    private String lastName;

    @Lob
    @Column(name = "profile_picture", columnDefinition = "LONGTEXT")
    @Setter
    private String profilePicture;

    @Column(nullable = false)
    @Setter
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public User() {
        // Intentionally empty: required by JPA/Hibernate to instantiate
        // this entity via reflection. No initialization needed here.
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now(ZoneId.systemDefault());
    }
}
