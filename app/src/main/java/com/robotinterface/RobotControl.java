package com.robotinterface;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class RobotControl extends AppCompatActivity {
    Handler activityHandler;
    Runnable inactivity;
    boolean isActivityPinned = false;
    MqttHandler mqttHandler = null;

    String TAG = "RobotControl";
    long inactivityTimeout = 5000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_control);

        // Get Objects
        try {
            final Object comHandler = ((ObjectWrapperForBinder)getIntent().getExtras().getBinder("mqttHandler")).getData();
            mqttHandler = (MqttHandler) comHandler;
        } catch (Exception ex) {
            Log.i(TAG, ex.getMessage());
        }

        connectButtons();

        // Activity Auto Close
        activityHandler = new Handler();
        inactivity = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Exited due to inactivity");
                finish();
            }
        };
        startTimer();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetTimer();
    }

    private void resetTimer() {
        stopTimer();
        if (!isActivityPinned)
        {
            startTimer();
        }
    }

    private void startTimer() {
        activityHandler.postDelayed(inactivity, inactivityTimeout);
    }

    private void stopTimer() {
        activityHandler.removeCallbacks(inactivity);
    }

    private void connectButtons()
    {
        // Pin Icon
        ImageView pinIcon = (ImageView) findViewById(R.id.pinIcon);
        pinIcon.setImageResource(getResources().getIdentifier("@drawable/unpin", null, getPackageName()));
        pinIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isActivityPinned) {
                    pinIcon.setImageResource(getResources().getIdentifier("@drawable/unpin", null, getPackageName()));
                    isActivityPinned = false;
                    startTimer();
                } else {
                    pinIcon.setImageResource(getResources().getIdentifier("@drawable/pin", null, getPackageName()));
                    isActivityPinned = true;
                    stopTimer();
                }
            }
        });

        // Power Off Button
        Button offButton = (Button) findViewById(R.id.offButton);
        offButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RobotControl.this);

                builder.setTitle("Confirm Action");
                builder.setMessage("Are you sure you want to power off Robot?");

                builder.setPositiveButton("PowerOff", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if (mqttHandler != null) {
                            mqttHandler.sendMessage("{\"command\":\"shutdown\"}", "robot_depart");
                        }
                        Log.i(TAG, "Power Off Robot");
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        // Gripper Release Button
        Button gripperReleaseButton = (Button) findViewById(R.id.gripperReleaseButton);
        gripperReleaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mqttHandler.sendMessage("{\"command\":\"gripper_extended_release\"}", "robot_depart");
                Log.i(TAG, "Release Gripper");
            }
        });
    }
}