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

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView text = (TextView) findViewById(R.id.label);
        ImageView imageView = (ImageView) findViewById(R.id.mouthView);

        Avatar avatar = new EmojiFace(this);

        // ROS Communication
        RobotCommunication.CallbackEvent mqttHandlerCallback = new RobotCommunication.CallbackEvent() {
            @Override
            public void onLogEvent(String msg) {
                text.setText(msg);
            }

            @Override
            public void onMessageReceivedEvent(String msg, String sender) {
                Log.i("Main", "onMessageReceivedEvent: ROS");
                text.setText("msg: " + msg + " from: " + sender);
                if (msg.equalsIgnoreCase("safety_off")) {
                    avatar.setMood(Avatar.MOOD.Angry);
                } else if (msg.equalsIgnoreCase("clear")) {
                    avatar.setMood(Avatar.MOOD.Happy);
                } else if (msg.equalsIgnoreCase("imminent_collision")) {
                    avatar.setMood(Avatar.MOOD.Sad);
                } else if (msg.equalsIgnoreCase("near_collision")) {
                    avatar.setMood(Avatar.MOOD.Nervous);
                } else {
                    avatar.setMood(Avatar.MOOD.Angry);
                }
            }
        };
        final MqttHandler mqttHandler = new MqttHandler(getApplicationContext(), "tcp://10.0.2.2:1883", "tablet", "obstacle_proximity", mqttHandlerCallback);
        //MqttHandler mqttHandler = new MqttHandler(getApplicationContext(), "tcp://192.168.2.127:1883", "tablet", "obstacle_proximity", mqttHandlerCallback);

        // I2R Communication
        RobotCommunication.CallbackEvent i2rMqttHandlerCallback = new RobotCommunication.CallbackEvent() {
            @Override
            public void onLogEvent(String msg) {
                text.setText(msg);
            }

            @Override
            public void onMessageReceivedEvent(String msg, String sender) {
                text.setText("msg: " + msg + " from: " + sender);
                Log.i("Main", "onMessageReceivedEvent: I2R");
                try {
                    JSONObject jObject = new JSONObject(msg);
                    String robot_status = jObject.getString("status");

                    if (robot_status.equalsIgnoreCase("error")) {
                        WebView webView = (WebView) findViewById(R.id.webView);
                        webView.setWebViewClient(new WebViewController());
                        webView.loadDataWithBaseURL("file:///android_asset/","<html><center><img src=\"on_fire.gif\"></html>","text/html","utf-8","");
                    } else {
                        WebView webView = (WebView) findViewById(R.id.webView);
                        webView.loadUrl("about:blank");
                    }
                } catch (Exception ex) {
                    text.setText("Cannot decode JSON from: " + sender + "[" + msg + "]  Reason: " + ex.getMessage());
                }
            }
        };
        final MqttHandler i2rMqttHandler = new MqttHandler(getApplicationContext(), "tcp://10.0.2.2:1883", "tablet2", "robot_status", i2rMqttHandlerCallback);
        //MqttHandler i2rMqttHandler = new MqttHandler(getApplicationContext(), "tcp://192.168.2.127:1883", "tablet2", "robot_status", i2rMqttHandlerCallback);



        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startRobotControl = new Intent(getApplicationContext(), RobotControl.class);
                final Bundle bundle = new Bundle();
                bundle.putBinder("mqttHandler", new ObjectWrapperForBinder(i2rMqttHandler));
                startRobotControl.putExtras(bundle);
                startActivity(startRobotControl);
            }
        });
    }

    private class WebViewController extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}