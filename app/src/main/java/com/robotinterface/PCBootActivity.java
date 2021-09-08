package com.robotinterface;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PCBootActivity extends AppCompatActivity {
    ImageView powerOnImageView;
    ImageView pcActivityImageView;
    Button powerOnNextButton;
    Button pcActivityYesButton;
    Button pcActivityNoButton;
    TextView powerOnTextView;
    TextView powerOnHintTextView;
    TextView pcActivityTextView;
    TextView pcActivityHintTextView;
    ProgressDialog bootProgressDialog;

    long timeOutValue = 20000L;

    int bootAttemptLimit = 2;
    int currentBootAttempts = 0;

    RobotAdaptor robot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcboot);
        TextView console = (TextView)findViewById(R.id.console_textView);

        // Initialize UI Components
        initUI();

        // Connect UI Components
        connectButtons();

        // Create Robot callback handlers
        RobotAdaptor.RobotCallback callback = new RobotAdaptor.RobotCallback() {
            @Override
            public void onLogEvent(String logMsg) {
                console.setText(logMsg);
            }

            @Override
            public void onRobotStatusCallback(RobotAdaptor.STATUS status) {
                if (status != RobotAdaptor.STATUS.Disconnected) {
                    finishStartup();
                }
            }

            @Override
            public void onRobotObstacleDetectionCallback(RobotAdaptor.OBSTACLE_DETECTION state) {
                // Pass
            }

        };
        // Create robot adaptor object
        robot = new RobotAdaptor(getApplicationContext(), callback);
    }

    void finishStartup() {
        bootProgressDialog.dismiss();
        Intent startRobotInitialPose = new Intent(getApplicationContext(), RobotInitialPoseActivity.class);
        final Bundle bundle = new Bundle();
        bundle.putBinder("robotAdaptor", new ObjectWrapperForBinder(robot));
        startRobotInitialPose.putExtras(bundle);
        startActivity(startRobotInitialPose);
    }

    void initUI() {
        powerOnImageView = (ImageView) findViewById(R.id.bootInstruction_imageView);
        pcActivityImageView = (ImageView) findViewById(R.id.bootInstruction_imageView2);
        powerOnNextButton = (Button) findViewById(R.id.bootInstruction_nextButton);
        pcActivityYesButton = (Button) findViewById(R.id.bootInstruction_yesButton);
        pcActivityNoButton = (Button) findViewById(R.id.bootInstruction_noButton);
        powerOnTextView = (TextView) findViewById(R.id.bootInstruction_textView);
        powerOnHintTextView = (TextView) findViewById(R.id.bootInstructionHint_textView);
        pcActivityTextView = (TextView) findViewById(R.id.bootInstruction_textView2);
        pcActivityHintTextView = (TextView) findViewById(R.id.bootInstructionHint_textView2);

        // Set visibilities
        pcActivityTextView.setVisibility(View.INVISIBLE);
        pcActivityImageView.setVisibility(View.INVISIBLE);
        pcActivityHintTextView.setVisibility(View.INVISIBLE);
        pcActivityYesButton.setVisibility(View.INVISIBLE);
        pcActivityNoButton.setVisibility(View.INVISIBLE);
        pcActivityNoButton.setClickable(false);
        pcActivityNoButton.setAlpha(0.5f);

        // Set Data
        powerOnImageView.setImageResource(getApplicationContext().getResources().getIdentifier("@drawable/pc_powered_off", null, getApplicationContext().getPackageName()));
        pcActivityImageView.setImageResource(getApplicationContext().getResources().getIdentifier("@drawable/pc_powered_on_booting", null, getApplicationContext().getPackageName()));

        // Configure Dialog Box
        bootProgressDialog = new ProgressDialog(PCBootActivity.this);
        bootProgressDialog.setMessage("Robot Primary Computer Booting! Please wait");
    }

    void connectButtons() {
        // Power On Next Button
        powerOnNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Change PC Image
                powerOnImageView.setImageResource(getApplicationContext().getResources().getIdentifier("@drawable/pc_powered_on", null, getApplicationContext().getPackageName()));

                // Set Visibility of next steps
                pcActivityTextView.setVisibility(View.VISIBLE);
                pcActivityImageView.setVisibility(View.VISIBLE);
                pcActivityHintTextView.setVisibility(View.VISIBLE);
                pcActivityYesButton.setVisibility(View.VISIBLE);
                pcActivityNoButton.setVisibility(View.VISIBLE);

                new CountDownTimer(timeOutValue, 1000L) {

                    public void onTick(long millisUntilFinished) {
                        pcActivityNoButton.setText("NO (" + (int)(millisUntilFinished/1000) + ")");
                    }

                    public void onFinish() {
                        pcActivityNoButton.setText("NO");
                        pcActivityNoButton.setClickable(true);
                        pcActivityNoButton.setAlpha(1f);
                    }
                }.start();
            }
        });


        // pcActivity Yes Button
        pcActivityYesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bootProgressDialog.show();
            }
        });

        // pcActivity No Button
        pcActivityNoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PCBootActivity.this);

                if (currentBootAttempts == 0) {
                    builder.setTitle("We ran into a problem!");
                    builder.setMessage("The boot process is having issues. Please press the PC Power button" +
                            " to turn off the Primary computer. Then press \"OK\" to re-attempt bootup");

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            initUI();
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    builder.setTitle("Oops! We are continuing to have issues");
                    builder.setMessage("We are very sorry for the inconvenience. " +
                            "Please turn off robot power switches (All 3 power switches) " +
                            " and turn them back on. Then re-attempt the boot process.");

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            initUI();
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                }

                currentBootAttempts++;
            }
        });
    }
}