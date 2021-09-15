package com.robotinterface;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;



public class RobotAdaptor extends AppCompatActivity {
    enum STATUS {
        Ok,
        Busy,
        Disabled,
        Error,
        Disconnected
    };

    enum OBSTACLE_DETECTION {
        Ok,
        Warning,
        Critical,
        Disabled
    }

    public interface  RobotCallback {
        public void onLogEvent(String logMsg);
        public void onRobotStatusCallback(STATUS status);
        public void onRobotObstacleDetectionCallback(OBSTACLE_DETECTION state);
    }

    Handler handler;
    Runnable runnable;
    MqttHandler mqttHandler;
    RobotCallback robotCallback = null;

    STATUS robotStatus;
    OBSTACLE_DETECTION obstacleStatus;
    boolean selfTestPassed;

    long watchdogTimeout = 13000L;

    Resources resources;
    String TAG = "RobotAdaptor";

    RobotAdaptor(Context context){
        // Initialize states
        robotStatus = STATUS.Disconnected;
        obstacleStatus = OBSTACLE_DETECTION.Ok;
        selfTestPassed = false;
        resources = context.getResources();

        // Robot Communication
        RobotCommunication.CallbackEvent mqttHandlerCallback = new RobotCommunication.CallbackEvent() {

            @Override
            public void onLogEvent(String logMsg) {
                RobotAdaptor.this.onLogEvent(logMsg);
            }

            @Override
            public void onMessageReceivedEvent(String msg, String sender) {
                //RobotAdaptor.this.onLogEvent("msg: " + msg + " from: " + sender);
                Log.d(TAG, "msg: " + msg + " from: " + sender);

                RobotAdaptor.this.onMessageReceivedEvent(msg, sender);
            }
        };

        mqttHandler = new MqttHandler(context, resources.getString(R.string.robot_ip), "tabletAdaptor", resources.getString(R.string.mqtt_robot_topic), mqttHandlerCallback);

        // Start Watchdog Timer
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                onWatchdogEvent();
            }
        };
        startTimer();
    }

    RobotAdaptor(Context context, RobotCallback callback)
    {
        this(context);
        setRobotCallback(callback);
    }

    public void onMessageReceivedEvent(String msg, String sender) {
            try {
                JSONObject jObject = new JSONObject(msg);
                String robot_name = jObject.getString(resources.getString(R.string.mqtt_robot_name_field));

                // If robot_name received, record heartbeat and reset watchdog timer
                resetTimer();
                if (robotStatus == STATUS.Disconnected) {
                    robotStatus = STATUS.Error;
                    onRobotStatusCallback(robotStatus);
                }

                // If robot_status is received, update robot state accordingly
                if (jObject.has(resources.getString(R.string.mqtt_robot_status_field)))
                {
                    String robot_status = jObject.getString(resources.getString(R.string.mqtt_robot_status_field));
                    onRobotStatusCallback(robot_status);
                }

                // If obstacle information is received, update obstacle information accordingly
                if (jObject.has(resources.getString(R.string.mqtt_robot_obstacle_detection_field)))
                {
                    String obstacle = jObject.getString(resources.getString(R.string.mqtt_robot_obstacle_detection_field));
                    onObstacleDetectionCallback(obstacle);
                }

                // If robot_self_test status is received, record it
                if (jObject.has(resources.getString(R.string.mqtt_robot_self_test_status_field)))
                {
                    selfTestPassed = jObject.getBoolean(resources.getString(R.string.mqtt_robot_self_test_status_field));
                }

            } catch (Exception ex) {
                String error_msg = "Cannot decode JSON from: " + sender + "[" + msg + "]  Reason: " + ex.getMessage();
                Log.e(TAG, error_msg);
                //onLogEvent(TAG + ": " + error_msg);
            }
    }

    private void onWatchdogEvent() {
        // If heartbeat is not received, watchdog event will be raised
        robotStatus = STATUS.Disconnected;
        onRobotStatusCallback(robotStatus);
    }

    private void resetTimer() {
        stopTimer();
        startTimer();
    }

    private void startTimer() {
        handler.postDelayed(runnable, watchdogTimeout);
    }

    private void stopTimer() {
        handler.removeCallbacks(runnable);
    }

    private void onLogEvent(String logMsg)
    {
        if (robotCallback != null) {
            robotCallback.onLogEvent(logMsg);
        }
    }

    private void onObstacleDetectionCallback (OBSTACLE_DETECTION state) {
        if (robotCallback != null) {
            robotCallback.onRobotObstacleDetectionCallback(state);
        }
    }

    private void onObstacleDetectionCallback (String obstacle) {
        if (obstacle.equalsIgnoreCase(resources.getString(R.string.mqtt_robot_obstacle_detection_ok_value))) {
            obstacleStatus = OBSTACLE_DETECTION.Ok;
        } else if (obstacle.equalsIgnoreCase(resources.getString(R.string.mqtt_robot_obstacle_detection_warn_value))) {
            obstacleStatus = OBSTACLE_DETECTION.Warning;
        } else if (obstacle.equalsIgnoreCase(resources.getString(R.string.mqtt_robot_obstacle_detection_critical_value))) {
            obstacleStatus = OBSTACLE_DETECTION.Critical;
        } else if (obstacle.equalsIgnoreCase(resources.getString(R.string.mqtt_robot_obstacle_detection_disabled_value))) {
            obstacleStatus = OBSTACLE_DETECTION.Disabled;
        } else {
            obstacleStatus = OBSTACLE_DETECTION.Disabled;
        }
        onObstacleDetectionCallback(obstacleStatus);
    }

    private void onRobotStatusCallback(STATUS status) {
        if (robotCallback != null) {
            robotCallback.onRobotStatusCallback(robotStatus);
        }
    }

    private void onRobotStatusCallback(String status)
    {
        // Robot State received. If previous state is different, report update
        if (status.equalsIgnoreCase("error")) {
            if (robotStatus != STATUS.Error) {
                robotStatus = STATUS.Error;
                onRobotStatusCallback(robotStatus);
            }
        } else if (status.equalsIgnoreCase("busy")) {
            if (robotStatus != STATUS.Busy) {
                robotStatus = STATUS.Busy;
                onRobotStatusCallback(robotStatus);
            }
        } else if (status.equalsIgnoreCase("disabled")) {
            if (robotStatus != STATUS.Disabled) {
                robotStatus = STATUS.Disabled;
                onRobotStatusCallback(robotStatus);
            }
        } else if (robotStatus != STATUS.Ok) {
            robotStatus = STATUS.Ok;
            onRobotStatusCallback(robotStatus);
        }
    }

    public void setRobotCallback(RobotCallback robotCallback) {
        this.robotCallback = robotCallback;

        // Send current robot status at callback registering time
        onRobotStatusCallback(robotStatus);
        // Send current obstacle status at callback registering time
        onObstacleDetectionCallback(obstacleStatus);
    }

    public void removeRobotCallback() {
        this.robotCallback = null;
    }

    public STATUS getRobotStatus() {
        return robotStatus;
    }

    public void releaseGripper() {
        String msg = "{\"" + resources.getString(R.string.robot_command_field) + "\":\"" +
                resources.getString(R.string.robot_command_GripperRelease) + "\"}";
        if (mqttHandler != null) {
            mqttHandler.sendMessage(msg, resources.getString(R.string.mqtt_command_topic));
        }
        onLogEvent("Release Gripper");
    }

    public void shutdown() {
        String msg = "{\"" + resources.getString(R.string.robot_command_field) + "\":\"" +
                resources.getString(R.string.robot_command_Shutdown) + "\"}";
        if (mqttHandler != null) {
            mqttHandler.sendMessage(msg, resources.getString(R.string.mqtt_command_topic));
        }
        onLogEvent("Release Gripper");
    }

    public void dockToCharger() {
        String msg = "{\"" + resources.getString(R.string.robot_command_field) + "\":\"" +
                resources.getString(R.string.robot_command_DockToCharger) + "\"}";
        if (mqttHandler != null) {
            mqttHandler.sendMessage(msg, resources.getString(R.string.mqtt_command_topic));
        }
        onLogEvent("Release Gripper");
    }

    public void startSelfTest() {
        String msg = "{\"" + resources.getString(R.string.robot_command_field) + "\":\"" +
                resources.getString(R.string.robot_command_StartSelfTest) + "\"}";
        if (mqttHandler != null) {
            mqttHandler.sendMessage(msg, resources.getString(R.string.mqtt_command_topic));
        }
        onLogEvent("Release Gripper");
    }
}
