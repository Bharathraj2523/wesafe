package com.example.thefinalapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProfilePage extends AppCompatActivity {

    private String userUid;
    private String name;
    private String phone;
    private String email;
    private TextView nameText;
    private TextView phoneText;
    private TextView emailText;
    private Button viewContactButton;
    private Button addContactButton;
    private DatabaseReference databaseReference;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        nameText = findViewById(R.id.name);
        phoneText = findViewById(R.id.phone);
        emailText = findViewById(R.id.email);
        viewContactButton=findViewById(R.id.viewContactButton);
        addContactButton=findViewById(R.id.addContactButton);

        addContactButton.setOnClickListener(v->addContact());
        viewContactButton.setOnClickListener(v->viewContact());

        Intent intent = getIntent();
        userUid = intent.getStringExtra("USER_UID");

        databaseReference = FirebaseDatabase.getInstance().getReference("Final_App").child(userUid).child("User_details");
        databaseReference.get().addOnCompleteListener(task -> {
           if(task.isSuccessful()){
                if(task.getResult().exists()){
                 UserData userData = task.getResult().getValue(UserData.class);
                 if(userData!=null){
                     name = userData.name;
                     phone = userData.phone;
                     email= userData.email;

                     nameText.setText("Name : "+name);
                     phoneText.setText("Phone : "+phone);
                     emailText.setText("Email : "+email);

                 }
                }else {
                    Toast.makeText(this, "No user data found", Toast.LENGTH_SHORT).show();
                }
           }else {
               Toast.makeText(this, "Failed to read data", Toast.LENGTH_SHORT).show();
           }
        });

    }

    private void viewContact(){
        View_Contact_Fragment viewContactFragment = new View_Contact_Fragment();
        viewContactFragment.show(getSupportFragmentManager(),"viewContactDialog");
    }
    private void addContact(){
        Add_Contact_Fragment addContactFragment = new Add_Contact_Fragment();
        addContactFragment.show(getSupportFragmentManager(),"addContactDialog");

    }
}
