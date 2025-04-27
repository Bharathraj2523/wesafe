package com.example.thefinalapp.contactDB;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ContactDAO {

    @Insert
    void insert(Contact contact);

    @Query("SELECT * FROM Contact")
    List<Contact> getAllContacts();

    @Delete
    void delete(Contact contact);
}
