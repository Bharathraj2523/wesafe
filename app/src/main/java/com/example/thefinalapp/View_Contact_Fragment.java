package com.example.thefinalapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thefinalapp.contactDB.AppDatabase;
import com.example.thefinalapp.contactDB.Contact;
import com.example.thefinalapp.contactDB.ContactDAO;

import java.util.List;

public class View_Contact_Fragment extends DialogFragment {
    private RecyclerView recyclerView;
    private ContactAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        // Only inflate and return the view here
        return inflater.inflate(R.layout.view_contact_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewContacts);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        AppDatabase db = AppDatabase.getInstance(requireContext());
        ContactDAO contactDAO = db.contactDAO();

        List<Contact> contactList = contactDAO.getAllContacts();

        Log.d("ContactDAO", "Contacts retrieved: " + contactList.size());
        for (Contact c : contactList) {
            Log.d("ContactDAO", "Name: " + c.name + ", Phone: " + c.phone);
        }

        adapter = new ContactAdapter(contactList);
        recyclerView.setAdapter(adapter);
    }
}
