package com.example.asuforia_library;

import android.media.Image;
import android.view.Surface;

public class Asuforia {

    PoseListener poseListener;
    Image image;
    Surface surface;

    Asuforia(PoseListener poseListener, Image im, Surface sf){
        this.poseListener = poseListener;
        this.image = im;
        this.surface = sf;
    }
}
