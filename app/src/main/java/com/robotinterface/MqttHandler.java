package com.robotinterface;


import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Timer;
import java.util.TimerTask;


public class MqttHandler implements RobotCommunication {
    MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
    MqttAndroidClient mqttAndroidClient;

    Context context;
    String serverURI;
    String topic;
    String clientId;
    CallbackEvent callback = null;

    public MqttHandler(Context context, String serverURI, String clientId) {

        this.context = context;
        this.serverURI = serverURI;
        this.clientId = clientId;

        mqttAndroidClient = new MqttAndroidClient(this.context, this.serverURI, this.clientId);
        // Connect Mqtt
        connect();
    }

    public MqttHandler(Context context, String serverURI, String clientId, String sub_topic, CallbackEvent callback) {
        this.context = context;
        this.serverURI = serverURI;
        this.clientId = clientId;
        this.topic = sub_topic;
        this.callback = callback;

        mqttAndroidClient = new MqttAndroidClient(this.context, this.serverURI, this.clientId);

        // Set callbacks
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log("Mqtt Connection Lost");
                reconnect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String msg = new String(message.getPayload());
                log("msg received: "+ msg);
                messageReceived(msg);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                log("Mqtt Msg delivered");
            }
        });

        // Connect Mqtt
        connect();
    }

    @Override
    public boolean sendMessage(String msg, String topic) {
        if (mqttAndroidClient.isConnected()) {
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
            message.setQos(1);
            try {
                mqttAndroidClient.publish(topic, message);
            } catch (Exception ex) {
                log ("Mqtt Publish Error: " + ex.getMessage());
            }
        } else {
            log ("Mqtt Not Connected. Cannot Send msg: " + msg + "\t topic: " + topic);
            return false;
        }
        return true;
    }

    private void connect() {
        try {
                mqttAndroidClient.connect(mqttConnectOptions,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    log("Mqtt connected");
                    subscribe(topic, callback);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    log("Mqtt Connection failed: " + exception.getMessage());
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void reconnect(){
        TimerTask task = new TimerTask() {
            public void run() {
                if (!mqttAndroidClient.isConnected()) {
                    connect();
                }
            }
        };
        Timer timer = new Timer("ReconnectionTimer");

        long delay = 2000L; // Milliseconds
        timer.schedule(task, delay);
    }

    private void subscribe(String topic, CallbackEvent callback){
        if (mqttAndroidClient.isConnected()){
            try {
                mqttAndroidClient.subscribe(this.topic, 1);
                log("Mqtt Subscribed to topic: " + topic);
            } catch (MqttException e) {
                log("Mqtt Subscription Failed");
            }
        }
    }

    private void log(String msg) {
        Log.i("MqttHandler", msg);
        if (callback != null){
            callback.onLogEvent(msg);
        }
    }

    private void messageReceived(String msg) {
        if (callback != null) {
            callback.onMessageReceivedEvent(msg, topic);
        }
    }
}
