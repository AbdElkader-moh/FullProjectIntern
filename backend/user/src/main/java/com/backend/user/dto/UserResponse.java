package com.backend.user.dto;

public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePicture;
    private String password;

    public UserResponse(Long id, String email, String firstName, String lastName, String profilePicture, String password) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePicture = profilePicture;
        this.password = password;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getProfilePicture() { return profilePicture; }
    public String getPassword() { return password; }
}