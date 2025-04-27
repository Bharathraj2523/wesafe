package com.example.thefinalapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Signup extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText nameField,phoneNumberField,emailField, passwordField;
    private TextInputLayout nameLayout,phoneNumberLayout,emailLayout, passwordLayout;
    private MaterialButton registerButton;
    private DatabaseReference databaseReference;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_layout);



        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        nameField = findViewById(R.id.nameField);
        phoneNumberField=findViewById(R.id.phoneNumberField);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);

        nameLayout=findViewById(R.id.nameLayout);
        phoneNumberLayout=findViewById(R.id.phoneNumberLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar); // Make sure to add this in your XML

        // Set click listeners
        registerButton.setOnClickListener(v -> registerUser());
        findViewById(R.id.loginLink).setOnClickListener(v -> {
            startActivity(new Intent(Signup.this, Login.class));
            finish();
        });
    }

    private void registerUser() {
        String name = nameField.getText().toString().trim();
        String phoneNumber = phoneNumberField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        // Reset errors
        nameLayout.setError(null);
        phoneNumberLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);

        // Validate email
        if(name.isEmpty()){
            nameLayout.setError("Name is required");
            nameField.requestFocus();
            return;
        }
        if(phoneNumber.isEmpty()){
            phoneNumberLayout.setError("Phone Number is required");
            phoneNumberField.requestFocus();
            return;
        }
        if (!Patterns.PHONE.matcher(phoneNumber).matches()){
            phoneNumberLayout.setError("Please enter a valid phone number");
            phoneNumberField.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            emailField.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Please enter a valid email");
            emailField.requestFocus();
            return;
        }

        // Validate password
        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            passwordField.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordLayout.setError("Minimum password length is 6 characters");
            passwordField.requestFocus();
            return;
        }
        //create a User
        UserData userData = new UserData(name,phoneNumber,email);
        // Show progress and disable button
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);

        // Create user with Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    registerButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Send verification email
                        // store name phone number under uid
                        FirebaseUser user = mAuth.getCurrentUser();
                        String uid = user.getUid();
                        databaseReference= FirebaseDatabase.getInstance().getReference("Final_App").child(uid).child("User_details");
                        databaseReference.setValue(userData)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Log.d("FirebaseDB", "User data successfully written.");
                                    } else {
                                        Log.e("FirebaseDB", "Failed to write user data", task.getException());
                                    }
                                });


                        sendEmailVerification();
                    } else {
                        // If sign up fails, display a message to the user
                        Toast.makeText(Signup.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendEmailVerification() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isEmailVerified()) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            //startActivity(new Intent(Signup.this, Login.class));
                            Toast.makeText(Signup.this, "Verification email sent to"+user.getEmail(), Toast.LENGTH_LONG).show();
                        } else {
                            Log.e("EMAIL", "Email not sent", task.getException());
                            Toast.makeText(Signup.this, "Email not sent", Toast.LENGTH_LONG).show();
                        }
                    });
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            startActivity(new Intent(this, Login.class));
            finish();
        }
    }
}