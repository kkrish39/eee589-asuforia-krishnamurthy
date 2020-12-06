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

    TextureView.SurfaceTextureListener surfaceTextureListener;
    CameraDevice cameraDevice;
    CameraManager cameraManager;
    CameraDevice.StateCallback stateCallback;
    CaptureRequest.Builder captureRequestBuilder;
    CameraCaptureSession cameraCaptureSession;
    ImageReader imageReader;
    Handler mBackgroundHandler;
    HandlerThread mBackgroundThread;
    Size imageDimensions;
    CaptureRequest previewRequest;

    Logger logger = Logger.getLogger(CameraInitializer.class.getName());

    private static final Integer IMAGE_WIDTH = 1200;
    private static final Integer IMAGE_HEIGHT = 2270;

    public CameraInitializer(Context context, PoseListener poseListener, TextureView textureView){
        this.context =context;
        this.poseListener = poseListener;
        this.textureView = textureView;
    }

    private void updatePreview() throws CameraAccessException {
        if(cameraDevice == null){
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        previewRequest = captureRequestBuilder.build();
        cameraCaptureSession.setRepeatingRequest(previewRequest, null, mBackgroundHandler);
    }

    public void initCameraDevice(){
        stateCallback = new CameraDevice.StateCallback() {
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

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {

                System.out.println("Surface texture listener --- - - - -");
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



        textureView.setSurfaceTextureListener(surfaceTextureListener);

        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }



    public void openCamera() throws CameraAccessException {
        String cameraId = cameraManager.getCameraIdList()[0];
        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        imageDimensions = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[2];

        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
//            ActivityCompat.requestPermissions(MainActivity, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
            logger.log(Level.SEVERE, "Not enough camera permissions");
            return;
        }
        Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888);

        imageReader = ImageReader.newInstance(IMAGE_HEIGHT,IMAGE_WIDTH,ImageFormat.YUV_420_888,2);
        imageReader.setOnImageAvailableListener(onImageAvailableListener,mBackgroundHandler);

        cameraManager.openCamera(cameraId,stateCallback,mBackgroundHandler);
    }


    private void createCameraPreview() throws CameraAccessException {
        System.out.println(" Camera Preview");
        SurfaceTexture surfaceTexture = this.textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(IMAGE_HEIGHT,IMAGE_WIDTH);
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        captureRequestBuilder.addTarget(imageReader.getSurface());


//        captureRequestBuilder.addTarget(sf);

        System.out.println();
        cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                if(cameraDevice == null){
                    return;
                }
                System.out.println("Am I entering here");
                cameraCaptureSession = session;
                try {
                    updatePreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        }, mBackgroundHandler);
    }

    private final ImageReader.OnImageAvailableListener onImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {

            try {
                Image im = reader.acquireLatestImage();

                float[] rotationAndTranslationVec = NativePoseEstimatorUtil.perform_pose_estimation(im.getPlanes()[0].getBuffer(), im.getWidth(),im.getHeight(), NativePoseEstimatorUtil.NUM_FEATURE_POINTS);

                if(rotationAndTranslationVec != null) {
                    System.out.println(rotationAndTranslationVec[0] + " " + rotationAndTranslationVec[1] + " " + rotationAndTranslationVec[2]);
                    System.out.println(rotationAndTranslationVec[3] + " " + rotationAndTranslationVec[4] + " " + rotationAndTranslationVec[5]);
                }

                poseListener.onPose(im, rotationAndTranslationVec);

                im.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    void startBackgroundThread(){
        mBackgroundThread = new HandlerThread("Camera Thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**/
    protected void stopBackgroundThread(){
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

