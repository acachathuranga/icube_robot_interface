package com.robotinterface;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RobotInitialPoseActivity extends AppCompatActivity {
    RobotAdaptor robot;
    Button dockButton;
    TextView console;
    String TAG = "SetInitialRobotPose";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_initial_pose);
        console = (TextView) findViewById(R.id.initialPoseConsole_textView);
        dockButton = (Button) findViewById(R.id.dock_Button);

        // Get Objects
        try {
            final Object robotAdaptor = ((ObjectWrapperForBinder)getIntent().getExtras().getBinder("robotAdaptor")).getData();
            robot = (RobotAdaptor) robotAdaptor;

            // Replace robot callbacks with activity methods
            RobotAdaptor.RobotCallback callback = new RobotAdaptor.RobotCallback() {
                @Override
                public void onLogEvent(String logMsg) {
                    RobotInitialPoseActivity.this.onLogEvent(logMsg);
                }

                @Override
                public void onRobotStatusCallback(RobotAdaptor.STATUS status) {
                    RobotInitialPoseActivity.this.onRobotStatusCallback(status);
                }

                @Override
                public void onRobotObstacleDetectionCallback(RobotAdaptor.OBSTACLE_DETECTION state) {
                    // Pass
                }
            };
            robot.setRobotCallback(callback);
            RobotInitialPoseActivity.this.onLogEvent("Callback Registered");

        } catch (Exception ex) {
            dockButton.setClickable(false);
            dockButton.setAlpha(0.5f);
            onLogEvent(ex.getMessage());
        }

        // Connect Buttons
        dockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                robot.dockToCharger();
                // Go to next activity
                Intent startMainActivity = new Intent(getApplicationContext(), MainActivity.class);
                final Bundle bundle = new Bundle();
                bundle.putBinder("robotAdaptor", new ObjectWrapperForBinder(robot));
                startMainActivity.putExtras(bundle);
                startActivity(startMainActivity);
            }
        });
    }

    private void onLogEvent(String logMsg){
        console.setText("SetRobotInitialPose: " + logMsg);
        Log.i(TAG, logMsg);
    }

    private void onRobotStatusCallback(RobotAdaptor.STATUS status){
        if (status != RobotAdaptor.STATUS.Disconnected){
            dockButton.setClickable(true);
            dockButton.setAlpha(1f);
        } else
        {
            dockButton.setClickable(false);
            dockButton.setAlpha(0.5f);
        }
    }
}