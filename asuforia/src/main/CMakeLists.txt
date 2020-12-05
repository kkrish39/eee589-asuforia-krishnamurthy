cmake_minimum_required(VERSION 3.4.1)
include_directories(/Users/keerthivasankrishnamurthy/Downloads/OpenCV-android-sdk/sdk/native/jni/include)
include(ndk-stl-config.cmake)
add_library( lib_opencv SHARED IMPORTED )
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_java4.so)

add_library( # Sets the name of the library.
        pose_estimation

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        cpp/pose_estimation.cpp)


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        pose_estimation

        lib_opencv
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
