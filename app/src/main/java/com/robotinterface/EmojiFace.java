package com.robotinterface;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import java.util.Random;

public class EmojiFace implements Avatar {
    Activity activity;
    ImageView faceView;
    ImageView eyesView;
    ImageView eyebrowsView;
    ImageView mouthView;

    Handler activityHandler;
    Runnable eyeAnimator;
    long eyeAnimationTime = 1000;
    long eyeAnimationInterval = 3000;


    String TAG = "EmojiFace";

    public EmojiFace(Activity activity){
        this.activity = activity;

        this.faceView = (ImageView) this.activity.findViewById(R.id.faceView);
        this.eyesView = (ImageView) this.activity.findViewById(R.id.eyesView);
        this.mouthView = (ImageView) this.activity.findViewById(R.id.mouthView);

        // Initialize Happy Face
        setImage(faceView, "@drawable/happy_face");
        setImage(eyesView, "@drawable/eyes");
        setImage(mouthView, "@drawable/happy_mouth");

        // Activity Handler
        activityHandler = new Handler();
        eyeAnimator = new Runnable() {
            @Override
            public void run() {
                float direction = (new Random().nextFloat() - 0.5f) * 2; // Random float [-1, 1]
                animateEyes(direction);
                startHandler();
            }
        };

        startHandler();
    }

    private void startHandler() {
        activityHandler.postDelayed(eyeAnimator, eyeAnimationInterval);
    }

    private void setImage(ImageView view, String image) {
        try {
            view.setImageResource(this.activity.getResources().getIdentifier(image, null, this.activity.getPackageName()));
        } catch (Exception ex) {
            Log.i(TAG, "setImage: " + ex.getMessage());
        }
    }

    @Override
    public void setMood(MOOD mood) {
        switch (mood) {

            case Happy:
                setImage(faceView, "@drawable/happy_face");
                setImage(eyesView, "@drawable/eyes");
                setImage(mouthView, "@drawable/happy_mouth");
                break;
            case Nervous:
                setImage(faceView, "@drawable/happy_face");
                setImage(eyesView, "@drawable/eyes");
                setImage(mouthView, "@drawable/nervous_mouth");
                break;
            case Angry:
                setImage(faceView, "@drawable/angry_face");
                setImage(eyesView, "@drawable/angry_eyes");
                setImage(mouthView, "@drawable/angry_mouth");
                break;
            case Sad:
                setImage(faceView, "@drawable/sad_face");
                setImage(eyesView,"@drawable/sad_eyes");
                setImage(mouthView, "@drawable/sad_mouth");
                break;
            case Sleepy:
                setImage(faceView, "@drawable/sleepy_face");
                setImage(eyesView, "@drawable/sleepy_eyes");
                setImage(mouthView, "@drawable/empty");
                break;
            case Dead:
                setImage(faceView, "@drawable/dead_face");
                setImage(eyesView, "@drawable/empty");
                setImage(mouthView, "@drawable/empty");
                break;
            default:
        }
    }

    /**
     *
     * @param direction -1 to 1 (-1: Left, 1: Right)
     */
    private void animateEyes(float direction) {
        float translation = direction * 50;
        ObjectAnimator animation = ObjectAnimator.ofFloat(eyesView, "translationX", translation);
        animation.setDuration(eyeAnimationTime);
        animation.start();
    }
}
