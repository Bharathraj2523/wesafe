package com.example.thefinalapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    Animation logo;
    ImageView content;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);
        content = findViewById(R.id.logoImage);
        logo = AnimationUtils.loadAnimation(this,R.anim.logo);

        startsplashscreen();

    }
    public void startsplashscreen(){
        content.startAnimation(logo);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this,Signup.class);
                startActivity(intent);
                finish();

            }
        } , 2000);
    }
}
