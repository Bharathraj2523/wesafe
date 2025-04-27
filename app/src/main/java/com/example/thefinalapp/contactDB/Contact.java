package com.example.thefinalapp.contactDB;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Contact")
public class Contact {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String phone;

    public Contact(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
    public int getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name=name;
    }
    public String getPhone(){
        return phone;
    }
    public void setPhone(String Phone){
        this.phone=phone;
    }
}
