package com.example.asuforia;

import android.media.Image;

public interface PoseListener {
    void onPose(/*Must populate with R ant T vectors and image data*/ Image image, float[] RTVector);

}