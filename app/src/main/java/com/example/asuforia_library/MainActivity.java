package com.example.asuforia_library;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;

import com.example.asuforia.Asuforia;
import com.example.asuforia.PoseListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity{
    Asuforia af;
    Image im;
    Surface sf;

    TextureView textureView;
    PoseListener poseListener;
    File reference_surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.textureview);

        try {

            InputStream is = getResources().openRawResource(R.raw.reference_surface);
            File directory = getDir("ref", Context.MODE_PRIVATE);
            reference_surface = new File(directory, "reference_surface.jpg");
            FileOutputStream os = new FileOutputStream(reference_surface);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        poseListener = new PoseListener() {
            @Override
            public void onPose(Image image) {
                System.out.println("Estimated Pose. About to color");

            }
        };


        System.out.println(reference_surface.getAbsolutePath());
        af = new Asuforia(poseListener, im, sf, this, textureView);
    }

    @Override
    protected void onStart() {
        System.out.println("Starting Pose Estimation");
        af.onResumeActivity();
        /* On entering foreground, start Estimation */
        try {
            af.startEstimation();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        super.onStart();
//        getDelegate().onStart();
    }

    @Override
    protected void onStop() {
        System.out.println("Ending Pose Estimation");
        af.onPauseActivity();
        /* On leaving foreground, End estimation */
        af.endEstimation();

        super.onStop();
//        getDelegate().onStop();
    }
}