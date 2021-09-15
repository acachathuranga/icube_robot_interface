package com.robotinterface;

public interface Avatar {
    enum MOOD{
        Happy,
        Nervous,
        Angry,
        Sleepy,
        Sad,
        Dead
    };
    public void setMood(MOOD mood);
}
