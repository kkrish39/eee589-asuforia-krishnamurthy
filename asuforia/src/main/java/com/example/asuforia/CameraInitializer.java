package com.example.asuforia;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CameraInitializer {

    Context context;
    PoseListener poseListener;
    TextureView textureView;


    CameraDevice cameraDevice;
    CaptureRequest.Builder captureRequestBuilder;
    CameraCaptureSession cameraCaptureSession;
    ImageReader imageReader;
    Handler mBackgroundHandler;
    HandlerThread mBackgroundThread;
    Size imageDimensions;

    Logger logger = Logger.getLogger(CameraInitializer.class.getName());

    public CameraInitializer(Context context, PoseListener poseListener, TextureView textureView){
        this.context =context;
        this.poseListener = poseListener;
        this.textureView = textureView;
    }

    private void updatePreview() throws CameraAccessException {
        if(cameraDevice == null){
            return;
        }

        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
    }

    public void initCameraDevice(){
        TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                try {
                    openCamera();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        };

        this.textureView.setSurfaceTextureListener(surfaceTextureListener);
    }



    public void openCamera() throws CameraAccessException {
        CameraManager cameraManager = (CameraManager) this.context.getSystemService(Context.CAMERA_SERVICE);

        String cameraId = cameraManager.getCameraIdList()[0];

        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        imageDimensions = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];

        if(ActivityCompat.checkSelfPermission(this.context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this.context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
//            ActivityCompat.requestPermissions(MainActivity, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
            logger.log(Level.SEVERE, "Not enough camera permissions");
            return;
        }
        Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888);

        for(int i=0;i<sizes.length;i++)
        {
            Log.d("Output Sizes ",""+sizes[i].getWidth()+" "+sizes[i].getHeight());
        }
        imageReader = ImageReader.newInstance(1280,640,ImageFormat.YUV_420_888,2);
        imageReader.setOnImageAvailableListener(onImageAvailableListener,mBackgroundHandler);

        cameraManager.openCamera(cameraId,stateCallback,mBackgroundHandler);
    }


    private void createCameraPreview() throws CameraAccessException {
        System.out.println(" Camera Preview");
        SurfaceTexture surfaceTexture = this.textureView.getSurfaceTexture();

        surfaceTexture.setDefaultBufferSize(1280,640);

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        captureRequestBuilder.addTarget(imageReader.getSurface());

        cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                if(cameraDevice == null){
                    return;
                }
                cameraCaptureSession = session;
                try {
                    updatePreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
//                Toast.makeText(getApplicationContext(),"Configuration Changed", Toast.LENGTH_LONG).show();
            }
        }, null);
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            try {
                createCameraPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private final ImageReader.OnImageAvailableListener onImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {

            try {
                Image im = reader.acquireLatestImage();
                System.out.println(im.getPlanes()[0]);
                poseListener.onPose(im);

                im.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
    };

    void startBackgroundThread() throws InterruptedException {
        mBackgroundThread = new HandlerThread("Camera Thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() throws InterruptedException {
        mBackgroundThread.quitSafely();
        mBackgroundThread.join();
        mBackgroundHandler = null;
    }


}
