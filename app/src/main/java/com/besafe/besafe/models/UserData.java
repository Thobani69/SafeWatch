package com.besafe.besafe.models;

import com.google.gson.annotations.SerializedName;

public class UserData {

    @SerializedName("student_number")
    private String studentNumber;

    @SerializedName("role")
    private String role;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("gender")
    private String gender;

    @SerializedName("faculty")
    private String faculty;

    @SerializedName("phone")
    private String phone;

    // Upgraded Constructor
    public UserData(String studentNumber, String role, String firstName, String lastName, String gender, String faculty, String phone) {
        this.studentNumber = studentNumber;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.faculty = faculty;
        this.phone = phone;
    }

    // Getters
    public String getRole() { return role; }
    public String getStudentNumber() { return studentNumber; }
    public String getPhone() { return phone; }
}