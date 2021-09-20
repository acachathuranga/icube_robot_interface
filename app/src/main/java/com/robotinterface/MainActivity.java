package com.robotinterface;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    RobotAdaptor robot;
    RobotAdaptor.RobotCallback callback;
    TextView console;
    String TAG = "Main";
    boolean isDisabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        console = (TextView) findViewById(R.id.label);
        ImageView imageView = (ImageView) findViewById(R.id.mouthView);

        Avatar avatar = new EmojiFace(this);

        try {
            final Object robotAdaptor = ((ObjectWrapperForBinder)getIntent().getExtras().getBinder("robotAdaptor")).getData();
            robot = (RobotAdaptor) robotAdaptor;
        } catch (Exception ex) {
            robot = new RobotAdaptor(getApplicationContext());
            onLogEvent(ex.getMessage());
        }

        // Replace robot callbacks with activity methods
        callback = new RobotAdaptor.RobotCallback() {
            @Override
            public void onLogEvent(String logMsg) {
                MainActivity.this.onLogEvent(logMsg);
            }

            @Override
            public void onRobotStatusCallback(RobotAdaptor.STATUS status) {
                MainActivity.this.onLogEvent("RobotStatus: " + status.name());
                if (status == RobotAdaptor.STATUS.Error) {
                    WebView webView = (WebView) findViewById(R.id.webView);
                    webView.setWebViewClient(new WebViewController());
                    webView.loadDataWithBaseURL("file:///android_asset/","<html><center><img src=\"on_fire.gif\"></html>","text/html","utf-8","");
                } else {
                    WebView webView = (WebView) findViewById(R.id.webView);
                    webView.loadUrl("about:blank");
                }
                Log.i("MainPrint", status.name());
                if (status == RobotAdaptor.STATUS.Disabled) {
                    avatar.setMood(Avatar.MOOD.Sleepy);
                } else if (status == RobotAdaptor.STATUS.Disconnected) {
                    avatar.setMood(Avatar.MOOD.Dead);
                } else {
                    avatar.setMood(Avatar.MOOD.Happy);
                }

                if (status == RobotAdaptor.STATUS.Disabled) {
                    isDisabled = true;
                } else {
                    isDisabled = false;
                }


            }

            @Override
            public void onRobotObstacleDetectionCallback(RobotAdaptor.OBSTACLE_DETECTION state) {
                MainActivity.this.onLogEvent("ObstacleDetection: " + state.name());
                if (isDisabled == false) {
                    switch (state) {
                        case Ok: avatar.setMood(Avatar.MOOD.Happy); break;
                        case Warning: avatar.setMood(Avatar.MOOD.Nervous); break;
                        case Critical: avatar.setMood(Avatar.MOOD.Sad); break;
                        default: avatar.setMood(Avatar.MOOD.Angry); break;
                    }
                }
            }
        };
        robot.setRobotCallback(callback);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startRobotControl = new Intent(getApplicationContext(), RobotControl.class);
                final Bundle bundle = new Bundle();
                bundle.putBinder("robotAdaptor", new ObjectWrapperForBinder(robot));
                startRobotControl.putExtras(bundle);
                startActivity(startRobotControl);
            }
        });
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

    private void onLogEvent(String logMsg) {
        console.setText(TAG + ": " + logMsg);
        Log.i(TAG, logMsg);
    }

    private class WebViewController extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}