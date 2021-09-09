package com.robotinterface;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class RobotControl extends AppCompatActivity {
    RobotAdaptor robot = null;
    RobotAdaptor.RobotCallback callback;
    Handler activityHandler;
    Runnable inactivity;
    ProgressDialog shutDownProgressDialog = null;
    AlertDialog pcOffAlert;

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
            final Object robotAdaptor = ((ObjectWrapperForBinder)getIntent().getExtras().getBinder("robotAdaptor")).getData();
            robot = (RobotAdaptor) robotAdaptor;

            callback = new RobotAdaptor.RobotCallback() {
                @Override
                public void onLogEvent(String logMsg) {
                    Log.i(TAG, logMsg);
                }

                @Override
                public void onRobotStatusCallback(RobotAdaptor.STATUS status) {
                    if (status == RobotAdaptor.STATUS.Disconnected) {
                        RobotControl.this.onShutdownComplete();
                    }
                }

                @Override
                public void onRobotObstacleDetectionCallback(RobotAdaptor.OBSTACLE_DETECTION state) {

                }
            };
            robot.setRobotCallback(callback);
        } catch (Exception ex) {
            Log.i(TAG, ex.getMessage());
        }

        connectButtons();

        // PC Off Alert
        AlertDialog.Builder builder = new AlertDialog.Builder(RobotControl.this);

        builder.setTitle("Robot Primary Computer Powered Off!");
        builder.setMessage("It is now safe to turn off robot power switches (All 3)");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        pcOffAlert = builder.create();

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
    protected void onRestart() {
        super.onRestart();
        robot.setRobotCallback(callback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        robot.setRobotCallback(callback);
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

    private void onShutdownComplete() {
        if (shutDownProgressDialog != null) {
            RobotControl.this.shutDownProgressDialog.dismiss();
            RobotControl.this.shutDownProgressDialog = null;
        }
        pcOffAlert.show();
    }

    private void startTimer() {
        activityHandler.postDelayed(inactivity, inactivityTimeout);
    }

    private void stopTimer() {
        activityHandler.removeCallbacks(inactivity);
    }

    private void pinActivity(boolean value) {
        ImageView pinIcon = (ImageView) findViewById(R.id.pinIcon);

        if (value) {
            pinIcon.setImageResource(getResources().getIdentifier("@drawable/pin", null, getPackageName()));
            stopTimer();
        } else {
            pinIcon.setImageResource(getResources().getIdentifier("@drawable/unpin", null, getPackageName()));
            startTimer();
        }
        isActivityPinned = value;
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
                    pinActivity(false);
                } else {
                    pinActivity(true);
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
                        if (robot != null) {
                            robot.shutdown();
                        }
                        Log.i(TAG, "Power Off Robot");
                        dialog.dismiss();
                        RobotControl.this.pinActivity(true);
                        RobotControl.this.shutDownProgressDialog = new ProgressDialog(RobotControl.this);
                        RobotControl.this.shutDownProgressDialog.setMessage("Robot Primary Computer Shutting down! Please wait");
                        RobotControl.this.shutDownProgressDialog.show();
                        if (robot.getRobotStatus() == RobotAdaptor.STATUS.Disconnected) {
                            onShutdownComplete();
                        }
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
                if (robot != null) {
                    robot.releaseGripper();
                }
                Log.i(TAG, "Release Gripper");
            }
        });
    }
}