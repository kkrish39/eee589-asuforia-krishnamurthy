#include <jni.h>
#include <string>
#include <opencv2/imgproc.hpp>
#include <opencv2/calib3d.hpp>
#include <opencv2/features2d.hpp>
#include <android/log.h>
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_asuforia_Asuforia_detect_1feature_1points(JNIEnv *env, jclass clazz, jstring reference_img_path, jint numFeaturePoints) {
    const char *path = env->GetStringUTFChars(reference_img_path, 0);


    __android_log_print(ANDROID_LOG_ERROR, "PoseEstimationMSA", "%s", "--------------------------------------");
    __android_log_print(ANDROID_LOG_ERROR, "PoseEstimationMSA", "%d", 80);


    __android_log_print(ANDROID_LOG_ERROR, "PoseEstimationMSA", "%s", "--------------------------------------");
//    Mat referenceImage = imread(path,0);
//
//    Ptr<FeatureDetector> detector = ORB::create(numFeautresReference,1.2f,8,31,0,2,ORB::HARRIS_SCORE,31,20);

    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_asuforia_Asuforia_perform_1pose_1estimation(JNIEnv *env, jclass clazz) {
    // TODO: implement perform_pose_estimation()

    return 0;
}