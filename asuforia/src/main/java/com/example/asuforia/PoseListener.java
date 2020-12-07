package com.example.asuforia;

import android.media.Image;

/*PoseListener Interface*/
public interface PoseListener {
    /*On pose method to paint the image with the cube
    * @image - image data
    * @RTVector - Rotational and Translational Vector
    * */
    void onPose(Image image, float[] RTVector);
    void onSurface();
}