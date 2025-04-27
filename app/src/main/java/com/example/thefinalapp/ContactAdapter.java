package com.example.thefinalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thefinalapp.contactDB.Contact;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {
    private List<Contact> contactList = new ArrayList<>();

    public ContactAdapter(List<Contact> contactList) {
        this.contactList = contactList;
        notifyDataSetChanged();
    }

    public void setContacts(List<Contact> contacts) {
        this.contactList = contacts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_list, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        holder.name.setText(contact.name);
        holder.phone.setText(contact.phone);
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView phone;
        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.contactName);
            phone = itemView.findViewById(R.id.contactPhone);
        }
    }
}
