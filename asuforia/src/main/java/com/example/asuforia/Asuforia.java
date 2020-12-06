package com.example.asuforia;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.view.TextureView;

public class Asuforia {
    private PoseListener poseListener;
    private CameraInitializer cameraInitializer;
    private TextureView textureView;

    /**/
    public Asuforia(PoseListener poseListener, String reference_image_path, Context context, TextureView textureView){
        this.poseListener = poseListener;
        this.textureView = textureView;

        cameraInitializer = new CameraInitializer(context,poseListener, textureView);
        cameraInitializer.initCameraDevice();

        System.out.println(reference_image_path);
        /*Call to find the feature points in the given reference image*/
//        detect_feature_points(reference_image_path,500);
        NativePoseEstimatorUtil.detect_feature_points(reference_image_path, NativePoseEstimatorUtil.NUM_FEATURE_POINTS);
        return;
    }

    public Asuforia() {}

    public void startEstimation() throws CameraAccessException {
        cameraInitializer.initCameraDevice();
    }

    public void onResumeActivity(){
        cameraInitializer.startBackgroundThread();


        if(this.textureView.isAvailable()) {
            try {
                cameraInitializer.openCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }else{
            this.textureView.setSurfaceTextureListener(cameraInitializer.surfaceTextureListener);
        }
    }

    public void onPauseActivity(){
        cameraInitializer.stopBackgroundThread();
    }

    public void endEstimation() throws InterruptedException {
        // Stop camera
        cameraInitializer.stopBackgroundThread();
    }
}

