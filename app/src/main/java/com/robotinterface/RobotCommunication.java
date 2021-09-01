package com.robotinterface;

public interface RobotCommunication {
    public interface CallbackEvent {
        public void onLogEvent (String logMsg);
        public void onMessageReceivedEvent (String msg, String sender);
    }
    public boolean sendMessage(String msg, String topic);
}
