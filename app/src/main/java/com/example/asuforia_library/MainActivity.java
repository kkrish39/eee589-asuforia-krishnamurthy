package com.example.asuforia_library;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import com.example.asuforia.Asuforia;
import com.example.asuforia.PoseListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Random;

public class MainActivity extends AppCompatActivity{
    private Asuforia af;

    private TextureView textureView;
    private PoseListener poseListener;
    private File reference_surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.textureview);

        try {
            /*Get the reference image from the local PC file system and convert it into the android file system*/
            InputStream inputStream = getResources().openRawResource(R.raw.reference_surface);
            File directory = getDir("ref", Context.MODE_PRIVATE);
            reference_surface = new File(directory, "reference_surface.jpg");
            FileOutputStream outputStream = new FileOutputStream(reference_surface);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        //TODO shoud remove
        Random r = new Random();

        poseListener = new PoseListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            /*On pose listener callback with the captured image and the Rotational and Translational vector */
            public void onPose(Image image, float[] RTVector) {

            }

        };

        /*Initialize the library with */
        af = new Asuforia(poseListener, reference_surface.getAbsolutePath(), MainActivity.this, textureView);
        try {
            af.startEstimation();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        af.onResumeActivity();
    }

    @Override
    protected void onStop() {
        super.onStop();
        af.onPauseActivity();
    }
}