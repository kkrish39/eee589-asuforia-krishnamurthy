#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
whatever(
        JNIEnv *env,
        jobject /* this */){
    std::string hello = "To be filled with pose estimation";
    return env->NewStringUTF(hello.c_str());
};