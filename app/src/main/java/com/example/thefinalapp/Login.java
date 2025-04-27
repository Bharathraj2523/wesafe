package com.example.thefinalapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText emailField, passwordField;
    private TextInputLayout emailLayout, passwordLayout;
    private MaterialButton loginButton;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> loginUser());

        findViewById(R.id.signupLink).setOnClickListener(v -> {
            startActivity(new Intent(Login.this, Signup.class));
            finish();
        });
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (!validateInputs(email, password)) {
            return;
        }

        showProgress(true);
        authenticateUser(email, password);
    }

    private boolean validateInputs(String email, String password) {
        boolean isValid = true;

        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            emailField.requestFocus();
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Please enter a valid email");
            emailField.requestFocus();
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            passwordField.requestFocus();
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Minimum password length is 6 characters");
            passwordField.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    private void authenticateUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    showProgress(false);

                    if (task.isSuccessful()) {
                        handleAuthenticationResult(mAuth.getCurrentUser());
                    } else {
                        showLoginError(task.getException().getMessage());
                    }
                });
    }

    private void handleAuthenticationResult(FirebaseUser user) {
        if (user != null && user.isEmailVerified()) {
            navigateToMainActivity(user);
        } else if (user != null) {
            promptEmailVerification();
        }
    }

    private void navigateToMainActivity(FirebaseUser user) {
        Intent intent = new Intent(Login.this,Home_page.class);
        intent.putExtra("USER_UID", user.getUid());
        intent.putExtra("USER_EMAIL", user.getEmail());
        startActivity(intent);
        finish();
    }

    private void promptEmailVerification() {
        Toast.makeText(this,
                "Please verify your email address first.",
                Toast.LENGTH_LONG).show();
    }

    private void showLoginError(String errorMessage) {
        String message = "Login failed: " + errorMessage;
        if (errorMessage.contains("invalid credential")) {
            message = "Invalid email or password";
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            navigateToMainActivity(currentUser);
        }
    }
}