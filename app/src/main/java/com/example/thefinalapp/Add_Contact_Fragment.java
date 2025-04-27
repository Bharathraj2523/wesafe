package com.example.thefinalapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.example.thefinalapp.contactDB.AppDatabase;
import com.example.thefinalapp.contactDB.Contact;


public class Add_Contact_Fragment extends DialogFragment {

    private EditText name;
    private EditText phone;
    private Button addContactButton;
    private ImageButton closeButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.add_contact_fragment,container,false);

        name = view.findViewById(R.id.editTextName);
        phone = view.findViewById(R.id.editTextPhone);
        addContactButton = view.findViewById(R.id.addContactButton);
        closeButton = view.findViewById(R.id.btnClose);

        addContactButton.setOnClickListener(v->{
            String Name = name.getText().toString().trim();
            String Phone = phone.getText().toString().trim();
            AppDatabase db = AppDatabase.getInstance(this.getContext());


            if(Name.isEmpty()||Phone.isEmpty()){
                Toast.makeText(requireContext(), "Enter the details correctly", Toast.LENGTH_SHORT).show();
                return;
            }
            db.contactDAO().insert(new Contact(Name,Phone));
            Toast.makeText(requireContext(), "Contact added successfully", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        closeButton.setOnClickListener(v->dismiss());
        return view;
    }


}
