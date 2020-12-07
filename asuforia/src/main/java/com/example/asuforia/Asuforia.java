package com.example.asuforia;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.view.TextureView;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Asuforia {
    Logger logger = Logger.getLogger(Asuforia.class.getName());


    private final CameraInitializer cameraInitializer;
    private final TextureView textureView;

    /*Entry point of the Asuforia library
    * @poseListener - Callback from the user program to paint the image with the given pose
    * @reference_image_path -  Path of the reference image used.
    * @context - MainActivity context
    * @textureView - texture view layout.
    * */
    public Asuforia(PoseListener poseListener, String reference_image_path, Context context, TextureView textureView){
        this.textureView = textureView;

        /*Initializes the instance of the camera fragment*/
        cameraInitializer = new CameraInitializer(context, poseListener, textureView);
        cameraInitializer.initCameraDevice();

        logger.log(Level.INFO, "Android File System path: "+reference_image_path);
        /*Call to find the feature points in the given reference image*/
        NativePoseEstimatorUtil.detectFeaturePoints(reference_image_path, NativePoseEstimatorUtil.NUM_FEATURE_POINTS);
    }

    /*Method to start the pose estimation process*/
    public void startEstimation() throws CameraAccessException {
        cameraInitializer.initCameraDevice();
    }

    /*Method to be called when app comes back to foreground*/
    public void onResumeActivity(){
        cameraInitializer.startBackgroundThread();

        if(this.textureView.isAvailable()) {
            try {
                cameraInitializer.openCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }else{
            this.textureView.setSurfaceTextureListener(cameraInitializer.getSurfaceTextureListener());
        }
    }

    /*Method to be called when app goes to background*/
    public void onPauseActivity(){
        cameraInitializer.stopBackgroundThread();
    }

    /*Method to end the pose estimation process*/
    public void endEstimation() throws InterruptedException {
        cameraInitializer.stopBackgroundThread();
    }
}

