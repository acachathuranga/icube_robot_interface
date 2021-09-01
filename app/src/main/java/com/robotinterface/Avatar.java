package com.robotinterface;

public interface Avatar {
    enum MOOD{
        Happy,
        Nervous,
        Angry,
        Sad
    };
    public void setMood(MOOD mood);
}
