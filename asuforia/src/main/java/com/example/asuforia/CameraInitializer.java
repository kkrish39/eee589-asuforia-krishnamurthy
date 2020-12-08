package com.example.asuforia;

import android.Manifest;
import android.app.Activity;
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
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/*Camera2 Fragment to start and listen to the image stream*/
public class CameraInitializer {

    private final Context context;
    private final PoseListener poseListener;
    private final TextureView textureView;

    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private ImageReader.OnImageAvailableListener onImageAvailableListener;
    private CameraDevice cameraDevice;
    private CameraManager cameraManager;
    private CameraDevice.StateCallback stateCallback;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imageReader;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Size imageDimensions;

    Logger logger = Logger.getLogger(CameraInitializer.class.getName());

    private static final Integer IMAGE_WIDTH = 1080;
    private static final Integer IMAGE_HEIGHT = 1920;
    private static final Integer MAX_IMAGES = 2;

    private static int count = 0;
    private static double movingAverage = 0;
//    private static
    public CameraInitializer(Context context, PoseListener poseListener, TextureView textureView){
        this.context =context;
        this.poseListener = poseListener;
        this.textureView = textureView;
    }

    /*Initialize camera2 device*/
    public void initCameraDevice(){

        /*Camera State Callback*/
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
                if(cameraDevice != null)
                    cameraDevice.close();
                cameraDevice = null;
            }
        };

        /*surface texture listener to trigger function onSurfaceAvailable and onSurfaceChange*/
        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
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

         /*Image listener callback that will be triggered when Image is available from the ImageReader*/
         onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                try {
                    Image im = reader.acquireLatestImage();

                    long startTime = System.currentTimeMillis();
                    float[] rotationAndTranslationVec = NativePoseEstimatorUtil.performPoseEstimation(im.getPlanes()[0].getBuffer(), im.getWidth(),im.getHeight(), NativePoseEstimatorUtil.NUM_FEATURE_POINTS);
                    poseListener.onPose(im, rotationAndTranslationVec);
                    long difference = System.currentTimeMillis() - startTime;
                    count++;
                    movingAverage = ((movingAverage*(count-1))+difference)/count;

                    logger.log(Level.INFO,"Count "+count+" "+ "Current Execution Time - "+difference+ "  Moving Average: "+movingAverage);
                    im.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        /*set listener to the textureView*/
        textureView.setSurfaceTextureListener(surfaceTextureListener);

        /*Initialize camera manager by requesting service from the android system*/
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }


    /*Method to open camera device*/
    public void openCamera() throws CameraAccessException {
        poseListener.onSurface();
        /*Get the camera I'd of the back camera*/
        String cameraId = cameraManager.getCameraIdList()[0];
        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        imageDimensions = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[2];

        logger.log(Level.INFO,"Surface Image Height: " + imageDimensions.getHeight());
        logger.log(Level.INFO,"Surface Image Width: " + imageDimensions.getWidth());

        /*Check if the camera permission is available. If not request permission and try opening the camera once again*/
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
            logger.log(Level.SEVERE, "Not enough camera permissions. Requesting !!");
            return;
        }

        /*imageDimensions.getWidth(),imageDimensions.getHeight()*/
        imageReader = ImageReader.newInstance(IMAGE_HEIGHT,IMAGE_WIDTH,ImageFormat.YUV_420_888,MAX_IMAGES);

        /*Set OnImageAvailable Listener*/
        imageReader.setOnImageAvailableListener(onImageAvailableListener,mBackgroundHandler);

        /*Open camera with the given cameraId and necessary callback with the background thread*/
        cameraManager.openCamera(cameraId,stateCallback,mBackgroundHandler);
    }

    /*Method to enable camera preview*/
    private void createCameraPreview() throws CameraAccessException {
        logger.log(Level.INFO, "Initializing Camera Preview");
        SurfaceTexture surfaceTexture = this.textureView.getSurfaceTexture();

        /*imageDimensions.getWidth(),imageDimensions.getHeight()*/
        surfaceTexture.setDefaultBufferSize(IMAGE_HEIGHT,IMAGE_WIDTH);
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        /*
        * Currently the camera is in capture mode, where it live relays the image data.
        * */
        captureRequestBuilder.addTarget(imageReader.getSurface());

        /*if surfaceTexture is added as a target, camera will move to preview mode.*/
//        captureRequestBuilder.addTarget(sf);

        cameraDevice.createCaptureSession(Collections.singletonList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
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

            }
        }, mBackgroundHandler);
    }

    private void updatePreview() throws CameraAccessException {
        if(cameraDevice == null){
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        CaptureRequest previewRequest = captureRequestBuilder.build();
        cameraCaptureSession.setRepeatingRequest(previewRequest, null, mBackgroundHandler);
    }

    /*Method to create background thread for the camera device and preview session to capture images*/
    void startBackgroundThread(){
        mBackgroundThread = new HandlerThread("Camera Thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /*Method to kill the background threads and close camera device*/
    protected void stopBackgroundThread(){
        closeDevice();
        if(mBackgroundThread != null) {
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


    private void closeDevice() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if(imageReader != null){
            imageReader.close();
            imageReader=null;
        }
    }

    /*getter method for surface listener to be used in Asuforia Library*/
    public TextureView.SurfaceTextureListener getSurfaceTextureListener(){
        return this.surfaceTextureListener;
    }
}

