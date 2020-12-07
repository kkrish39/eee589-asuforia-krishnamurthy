package com.example.asuforia;

import java.nio.ByteBuffer;

/*Util class to call Native OpenCV function*/
public class NativePoseEstimatorUtil {
    static {
        System.loadLibrary("pose_estimation");
    }

    public static final Integer NUM_FEATURE_POINTS = 4000;

    /*Private Constructor to avoid initializing the util class*/
    private NativePoseEstimatorUtil(){}

    /* wrapper for the native util functions that will be used inside the asuforia library*/
    public static int detectFeaturePoints(String reference_img_path, int numFeaturePoints){
        return detect_feature_points(reference_img_path,numFeaturePoints);
    }

    public static float[] performPoseEstimation(ByteBuffer image_buffer, int width, int height, int numFeaturePoints){
        return perform_pose_estimation(image_buffer,width,height,numFeaturePoints);
    }
    /*
        Function to generate the feature descriptors and feature points from the reference image
        @reference_img_path - Path of the reference image in android file system
        @numFeaturePoints - Number of feature points of interest
     */
    private static native int detect_feature_points(String reference_img_path, int numFeaturePoints);

    /*Function to estimate pose with respect to the reference image
    * @imageBuffer - Image data buffer
    * @height - Height of the image
    * @width - Width of the image
    * */
    private static native float[] perform_pose_estimation(ByteBuffer image_buffer, int width, int height, int numFeaturePoints);
}
