package com.example.asuforia_library;

import androidx.appcompat.app.AppCompatActivity;

import android.media.Image;
import android.os.Bundle;
import android.view.Surface;

public class MainActivity extends AppCompatActivity{
    Asuforia af;
    Image im;
    Surface sf;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PoseListener poseListener = new PoseListener() {
            @Override
            public void onPose() {
                System.out.println("Estimated Pose. About to color");
            }
        };

        af = new Asuforia(poseListener, im, sf);
    }

    @Override
    protected void onStart() {
        System.out.println("Starting Pose Estimation");

        /* On entering foreground, start Estimation */
        af.startEstimation();

        super.onStart();
//        getDelegate().onStart();
    }

    @Override
    protected void onStop() {
        System.out.println("Ending Pose Estimation");

        /* On leaving foreground, End estimation */
        af.endEstimation();

        super.onStop();
//        getDelegate().onStop();
    }
}