package com.example.asuforia;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.view.Surface;
import android.view.TextureView;

public class Asuforia {

    PoseListener poseListener;
    Image image;
    Surface surface;
    CameraInitializer cameraInitializer;
    TextureView textureView;

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("pose_estimation");
    }

    public Asuforia(PoseListener poseListener, Image im, Surface sf, Context context, TextureView textureView){
        this.poseListener = poseListener;
        this.image = im;
        this.surface = sf;
        this.textureView = textureView;

        cameraInitializer = new CameraInitializer(context,poseListener, textureView);
        cameraInitializer.initCameraDevice();

        /*Call to find the feature points in the given reference image*/
        detect_feature_points("",360);
    }

    public Asuforia() {}

    public void onImageAvailable(){
        /*Native pose Estimation call to find R and T vectors*/
        perform_pose_estimation();

//        this.poseListener.onPose(/*Must populate with R ant T vectors and image data*/);
    }

    public void startEstimation() throws CameraAccessException {
        // Start camera
        onImageAvailable();
    }

    public void onResumeActivity(){
        try {
            cameraInitializer.startBackgroundThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(this.textureView.isAvailable()) {
            try {
                cameraInitializer.openCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void onPauseActivity(){
        try {
            cameraInitializer.stopBackgroundThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void endEstimation(){
        // Stop camera
    }

    /* Dummy Native functions */
    public static native int detect_feature_points(String reference_img_path, int numFeaturePoints);
    public static native int perform_pose_estimation();
}
