package com.example.asuforia_library;

import android.view.Surface;
import java.nio.ByteBuffer;

/*Util class to draw cube on the given image*/
public class DrawPoseUtil {

    static {
        System.loadLibrary("draw_cube");
    }

    /*Avoid Instantiating Util Class*/
    private DrawPoseUtil(){ }

    public static void drawCube(ByteBuffer buffer, Surface surface,  int width, int height, float[] RTVector){
        drawCubeNative(buffer, surface, width, height, RTVector);
    }

    /*Native function to draw cube over the image
    *
    * @buffer - Image buffer
    * @surface - textureView surface
    * @width - Width of the Image
    * @height - Height of the Image
    * @RTVector - Rotational and Translational Vector
    * */
    private static native void drawCubeNative(ByteBuffer buffer, Surface surface,  int width, int height, float[] RTVector);
}
