package com.example.thefinalapp;

public class UserData {
    public String name;
    public String email;
    public String phone;

    // Default constructor required for calls to DataSnapshot.getValue(UserData.class)
    public UserData() {
    }

    public UserData(String name, String phone, String email) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    // Getters and setters (optional, but recommended for encapsulation)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
