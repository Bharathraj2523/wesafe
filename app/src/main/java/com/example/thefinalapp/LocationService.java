package com.example.thefinalapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private boolean isRunning = false;
    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler;
    private Runnable locationRunnable;
    public String res;
    private static final String PHONE_NUMBER = "6381141829"; // Replace with actual number
    private static final String SMS_MESSAGE = "Emergency alert! User is in danger zone!";

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        startForegroundService();
        isRunning = true;

        handler = new Handler();
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    getLastLocation();
                    // Repeat the task every 60 seconds (60000 ms)
                    handler.postDelayed(this, 60000);
                }
            }
        };

        // Start the periodic location fetching
        handler.post(locationRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Getting Location")
                .setContentText("Location service is running...")
                .setSmallIcon(R.drawable.ic_location)  // <-- Make sure this icon exists in your resources
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                String day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
                String path = getFilesDir().getAbsolutePath();
                String result = sendLocationToPython(location.getLatitude(), location.getLongitude(), time, day , path);
                res = result;
                if(result.equalsIgnoreCase("alert")){
                    //check bpm also
                    checkbpm(result.toString());
                    //send sms or call to a particlar number
                    checkAndSendAlert();
                }

                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Python result: " + result);
            } else {
                checkbpm("null");
                Log.e(TAG, "Location is null");
               // Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show();

            }
        }).addOnFailureListener(e -> {

            Log.e(TAG, "Failed to get location: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

        });
    }

    private void checkAndSendAlert() {
        try {
            Thread.sleep(5000);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                            == PackageManager.PERMISSION_GRANTED) {

                sendSMS();
                makePhoneCall();

            } else {
                // Handle case where permissions are not granted
                Log.e(TAG, "SMS/Call permissions not granted");
                Toast.makeText(this, "Cannot send alert - permissions missing", Toast.LENGTH_LONG).show();
            }
        }
        catch(Exception e){

        }
    }

    private void sendSMS() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(PHONE_NUMBER, null, SMS_MESSAGE, null, null);
            Log.d(TAG, "SMS sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "SMS sending failed: " + e.getMessage());
        }
    }
    private  void checkbpm(String result){

        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(this));
            }

            Python py = Python.getInstance();
            PyObject pyObject = py.getModule("predicting");
            PyObject results = pyObject.callAttr("checkbpm");
            if(results.toString().equalsIgnoreCase("high bpm")){
                if(result.equalsIgnoreCase("alert")){
                    Toast.makeText(this, "HIGH HEART RATE DETECTED", Toast.LENGTH_SHORT).show();

                    checkAndSendAlert();
                }
                else{
                    Toast.makeText(this, "HIGH HEART RATE DETECTED", Toast.LENGTH_SHORT).show();
                    checkAndSendAlert();
                }


            }

        } catch (Exception e) {
            Log.e(TAG, "Python error: " + e.getMessage(), e);

        }
    }

    private void makePhoneCall() {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + PHONE_NUMBER));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(callIntent);
            Log.d(TAG, "Call initiated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Call failed: " + e.getMessage());
        }
    }


    private String sendLocationToPython(double latitude, double longitude, String time, String day , String path) {
        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(this));
            }

            Python py = Python.getInstance();
            PyObject pyObject = py.getModule("predicting");
            PyObject result = pyObject.callAttr("process_location", latitude, longitude, time, day , path);
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Python error: " + e.getMessage(), e);
            return "Error calling Python: " + e.getMessage();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        handler.removeCallbacks(locationRunnable); // Stop the handler when the service is destroyed
        super.onDestroy();
    }
}
