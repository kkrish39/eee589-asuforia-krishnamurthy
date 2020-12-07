#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <algorithm>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

/*Scaling parameter for the focal length. Increasing and decreasing the value will change the size of the cube*/
const double SCALE_PARAMETER = 2.0;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_asuforia_1library_DrawPoseUtil_drawCubeNative(JNIEnv *env, jclass clazz, jobject image_buffer,
                                                               jobject surface, jint width, jint height,
                                                               jfloatArray rtvector) {

    jfloat *RTVector;

    /*Check if the the Rotational and Translational Vector is null*/
    if(rtvector != nullptr){
        RTVector = env->GetFloatArrayElements(rtvector, nullptr);
    }

    /*Get the starting pointer for the image buffer*/
    auto *sourceInputBuffer = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(image_buffer));

    /*Read the image in raw YUV format*/
    Mat originalYUVImage(height+height/2, width, CV_8UC1, sourceInputBuffer);

    /*Convert the image to a grayscaled image*/
    Mat grayScaledImage(height,width,CV_8UC1,sourceInputBuffer);

    /*Acquire lock to the texture surface*/
    ANativeWindow *win = ANativeWindow_fromSurface(env, surface);
    ANativeWindow_acquire(win);

    ANativeWindow_Buffer buf;
    int destinationImgWidth = height;
    int destinationImgHeight = width;

    /*Set the size of the surface based on the destination buffer size*/
    ANativeWindow_setBuffersGeometry(win, destinationImgWidth, destinationImgHeight, 0 );

    /*Check if there is a lock alredy on the surface. If yes, terminate*/
    if (int32_t err = ANativeWindow_lock(win, &buf, NULL)) {
        ANativeWindow_unlockAndPost(win);
        ANativeWindow_release(win);
        return;
    }

    /*Initializing the pointer to the target Image*/
    auto *targetImagePointer = reinterpret_cast<uint8_t *>(buf.bits);

    /*Create Matrices to copy and manipulate the image buffer*/
    Mat sourceImageMatrix(height, width, CV_8UC4);
    Mat targetImageMatrix(destinationImgHeight, buf.stride, CV_8UC4,targetImagePointer);
    Mat flipImageMatrix(destinationImgHeight, destinationImgWidth, CV_8UC4);
    Mat colorImageMatrix(destinationImgHeight, destinationImgWidth, CV_8UC4);


    /*Convert the YUV image to RGB Scale*/
    cvtColor(originalYUVImage, colorImageMatrix, COLOR_YUV2RGBA_NV21);

    if(rtvector!= nullptr) {
        /*Calculate the center of the image based on the image height and width*/
        const double centerCordinateX = (double) grayScaledImage.rows / 2;
        const double centerCordinateY = (double) grayScaledImage.cols / 2;

        /*Calculate the focal point co-ordinaties with certain scaling value*/
        const double focalCordinateX = centerCordinateX * SCALE_PARAMETER;
        const double focalCordinateY = centerCordinateY * SCALE_PARAMETER;


        vector<Point3d> cubeAxes;
        int cubeAxisLength = 5;

        cubeAxes.emplace_back(0, 0, 0);
        cubeAxes.emplace_back(0, cubeAxisLength, 0);
        cubeAxes.emplace_back(cubeAxisLength, cubeAxisLength, 0);
        cubeAxes.emplace_back(cubeAxisLength, 0, 0);
        cubeAxes.emplace_back(0, 0, -cubeAxisLength);
        cubeAxes.emplace_back(0, cubeAxisLength, -cubeAxisLength);
        cubeAxes.emplace_back(cubeAxisLength, cubeAxisLength, -cubeAxisLength);
        cubeAxes.emplace_back(cubeAxisLength, 0, -cubeAxisLength);

        double cameraCoefficentVector[9] = {focalCordinateX, 0, centerCordinateX, 0,
                                            focalCordinateY, centerCordinateY, 0, 0, 1};
        Mat cameraCoefficentMatrix = Mat(3, 3, CV_64F, cameraCoefficentVector);

        vector<Point2d> imagePoints;

        double RVector[3]={double(RTVector[0]),double(RTVector[1]),double(RTVector[2])};
        double TVector[3]={double(RTVector[3]),double(RTVector[4]),double(RTVector[5])};

        Mat RVectorMatrix = Mat(3, 1, CV_64F, RVector);
        Mat TVectorMatrix = Mat(3, 1, CV_64F, TVector);

        projectPoints(cubeAxes, RVectorMatrix, TVectorMatrix, cameraCoefficentMatrix, Mat::zeros(5, 1, CV_64F),
                      imagePoints);

        vector<vector<Point>> baseCubePoints(1);

        for (int i = 0; i < 4; i++) {
            baseCubePoints[0].push_back(Point((int) imagePoints[i].x, (int) imagePoints[i].y));
        }

        Scalar lineColor = Scalar (0,0,255);
        drawContours(colorImageMatrix, baseCubePoints,-1,(0,255,255), FILLED, LINE_8);

        for(int i=0;i<4;i++){
            line(colorImageMatrix,imagePoints[i],imagePoints[i+4],lineColor,3);
        }

        line(colorImageMatrix,imagePoints[0],imagePoints[1],lineColor,3);
        line(colorImageMatrix,imagePoints[1],imagePoints[2],lineColor,3);
        line(colorImageMatrix,imagePoints[2],imagePoints[3],lineColor,3);
        line(colorImageMatrix,imagePoints[3],imagePoints[0],lineColor,3);
        line(colorImageMatrix,imagePoints[4],imagePoints[5],lineColor,3);
        line(colorImageMatrix,imagePoints[5],imagePoints[6],lineColor,3);
        line(colorImageMatrix,imagePoints[6],imagePoints[7],lineColor,3);
        line(colorImageMatrix,imagePoints[7],imagePoints[4],lineColor,3);
    }

    transpose(colorImageMatrix, colorImageMatrix);
    flip(colorImageMatrix, colorImageMatrix,1);


    uchar *destinationBuffer;
    uchar *sourceBuffer;
    sourceBuffer = colorImageMatrix.data;
    for (int i = 0; i < colorImageMatrix.rows; i++) {
        destinationBuffer = targetImageMatrix.data + i * buf.stride * 4;
        memcpy(destinationBuffer, sourceBuffer, colorImageMatrix.cols*4);
        sourceBuffer += colorImageMatrix.cols * 4;
    }

    ANativeWindow_unlockAndPost(win);
    ANativeWindow_release(win);

    /*Release memory*/
    sourceImageMatrix.release();
    targetImageMatrix.release();
    originalYUVImage.release();
    grayScaledImage.release();
    flipImageMatrix.release();
    colorImageMatrix.release();


    if(rtvector != nullptr){env->ReleaseFloatArrayElements(rtvector, RTVector, 0);}
}