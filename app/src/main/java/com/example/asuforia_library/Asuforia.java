package com.example.asuforia_library;

import android.media.Image;
import android.view.Surface;

public class Asuforia {

    PoseListener poseListener;
    Image image;
    Surface surface;

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("pose_estimation");
    }

    Asuforia(PoseListener poseListener, Image im, Surface sf){
        this.poseListener = poseListener;
        this.image = im;
        this.surface = sf;

        /*Call to find the feature points in the given reference image*/
        detect_feature_points(/*Must be called with the reference image data and number of feature points of choice*/);
    }

    public Asuforia() {}

    void onImageAvailable(){
        /*Native pose Estimation call to find R and T vectors*/
        perform_pose_estimation();

        this.poseListener.onPose(/*Must populate with R ant T vectors and image data*/);
    }

    void startEstimation(){
        // Start camera
        onImageAvailable();
    }

    void endEstimation(){
        // Stop camera
    }

    /* Dummy Native functions */
    public static native int detect_feature_points();
    public static native int perform_pose_estimation();
}
