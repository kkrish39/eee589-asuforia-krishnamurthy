package com.example.asuforia_library;

import android.media.Image;
import android.view.Surface;

public class Asuforia {

    PoseListener poseListener;
    Image image;
    Surface surface;

    static {
        System.loadLibrary("opencv_java4");
    }

    Asuforia(PoseListener poseListener, Image im, Surface sf){
        this.poseListener = poseListener;
        this.image = im;
        this.surface = sf;
    }

    public Asuforia() {}

    void onImageAvailable(){
        //Call Native pose Estimation
        this.poseListener.onPose();
    }

    void startEstimation(){
        // Start camera
        onImageAvailable();
    }

    void endEstimation(){
        // Stop camera
    }
}
