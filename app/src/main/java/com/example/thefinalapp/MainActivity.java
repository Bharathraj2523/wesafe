package com.example.thefinalapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = "MainActivity";
    private String userUid;
    private String userEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        userUid = intent.getStringExtra("USER_UID");
        userEmail = intent.getStringExtra("USER_EMAIL");
// Verify we have a valid user
        if (userUid == null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.isEmailVerified()) {
                userUid = currentUser.getUid();
                userEmail = currentUser.getEmail();
            } else {
                // No valid user - return to Login
                Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Login.class));
                finish();
                return;
            }
        }

        Log.d("MainActivity", "Starting for user: " + userEmail);


        // Initialize Python if not started
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // Run the Python script for pre-processing
        runPythonFunction();

        // Ask for location permissions
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            startLocationService(); // Start service if permissions already granted
        }
    }

    private void runPythonFunction() {
        try {
            Python py = Python.getInstance();
            String path = getFilesDir().getAbsolutePath();
            PyObject result = py.getModule("predicting").callAttr("fetch_summary_and_save", path);
            String status = result.toString();

            Toast.makeText(this, status.equals("success") ?
                    "Summary fetched and saved!" : "Failed: " + status, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Python error: ", e);
            Toast.makeText(this, "Python error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkPermissions() {
        boolean fineLoc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean bgLoc = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bgLoc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        return fineLoc && bgLoc;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    }, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Log.d(TAG, "Location service started");
        Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show();
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
        Log.d(TAG, "Location service stopped");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE && checkPermissions()) {
            startLocationService();
        } else {
            Toast.makeText(this, "Permissions denied. Cannot track location.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationService();
    }
}
