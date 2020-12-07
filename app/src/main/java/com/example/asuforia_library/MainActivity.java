package com.example.asuforia_library;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.example.asuforia.Asuforia;
import com.example.asuforia.PoseListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity{
    private Logger logger = Logger.getLogger(MainActivity.class.getName());

    private Asuforia af;

    private TextureView textureView;
    private File reference_surface;

    Surface[] surface = new Surface[1];

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

        /*On pose listener callback with the captured image and the Rotational and Translational vector */
        PoseListener poseListener = new PoseListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            /*On pose listener callback with the captured image and the Rotational and Translational vector */
            public void onPose(Image image, float[] RTVector) {
                DrawPoseUtil.drawCube(image.getPlanes()[0].getBuffer(), surface[0], image.getWidth(),image.getHeight(), RTVector);
            }

            @Override
            public void onSurface() {
                surface[0] = new Surface(textureView.getSurfaceTexture());
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 101){
            if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                Toast.makeText(getApplicationContext(), "Required: Camera Permission", Toast.LENGTH_LONG).show();
                try {
                    af.endEstimation();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                af.onResumeActivity();
            }
        }
    }

    /*Background listener to resume the process on Foreground*/
    @Override
    protected void onResume() {
        logger.log(Level.INFO,"App is on Foreground");
        super.onResume();
        af.onResumeActivity();

    }

    /*Background listener to stop the process on Background*/
    @Override
    protected void onPause() {
        logger.log(Level.INFO,"App is on background");
        super.onPause();
        af.onPauseActivity();
        surface[0] = null;
    }

    //    @Override
//    protected void onStart() {
//        super.onStart();
//        af.onResumeActivity();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        System.out.println("Closing app -----");
//        surface = null;
//    }

}