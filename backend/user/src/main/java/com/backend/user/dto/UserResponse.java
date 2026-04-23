package com.backend.user.dto;

public class UserResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePicture;

    public UserResponse() {
    }

    public UserResponse(Long id, String email, String firstName, String lastName, String profilePicture) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePicture = profilePicture;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getProfilePicture() {
        return profilePicture;
    }
}