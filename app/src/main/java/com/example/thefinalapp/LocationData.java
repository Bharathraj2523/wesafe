package com.example.thefinalapp;


public class LocationData {
    public double latitude;
    public double longitude;
    public String time;
    public String day;

    public LocationData() {} // Needed for Firebase

    public LocationData(double latitude, double longitude, String time, String day) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
        this.day = day;
    }
}

