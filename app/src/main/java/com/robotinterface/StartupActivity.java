package com.robotinterface;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class StartupActivity extends AppCompatActivity {
    long inactivityTimeout = 5000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        // Play boot sound
        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.startup_tone);
        mediaPlayer.start(); // no need to call prepare(); create() does that for you

        // Continue to next activity after delay
        Handler activityHandler = new Handler();
        Runnable inactivity = new Runnable() {
            @Override
            public void run() {
                Intent startPCBootActivity = new Intent(getApplicationContext(), PCBootActivity.class);
                startActivity(startPCBootActivity);
            }
        };
        activityHandler.postDelayed(inactivity, inactivityTimeout);
    }
}