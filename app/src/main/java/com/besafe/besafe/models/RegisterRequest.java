package com.besafe.besafe.models;

public class RegisterRequest {
    private String email;
    private String password;
    private UserData data; // This holds our custom data from above

    public RegisterRequest(String email, String password, UserData data) {
        this.email = email;
        this.password = password;
        this.data = data;
    }
}