package com.example.asuforia;

import java.nio.ByteBuffer;

public class NativePoseEstimatorUtil {
    static {
        System.loadLibrary("pose_estimation");
    }

    public static final Integer NUM_FEATURE_POINTS = 1000;

    /*Private Constructor to avoid initializing the util class*/
    private NativePoseEstimatorUtil(){}

    /* static native util functions that will be used inside the asuforia library*/

    /*
        Function to generate the feature descriptors and feature points from the reference image
        @reference_img_path - Path of the reference image in android file system
        @numFeaturePoints - Number of feature points of interest
     */
    public static native int detect_feature_points(String reference_img_path, int numFeaturePoints);

    /*Function to estimate pose with respect to the reference image
    * @imageBuffer - Image data buffer
    * @height - Height of the image
    * @width - Width of the image
    * */
    public static native float[] perform_pose_estimation(ByteBuffer image_buffer, int width, int height, int numFeaturePoints);
}
