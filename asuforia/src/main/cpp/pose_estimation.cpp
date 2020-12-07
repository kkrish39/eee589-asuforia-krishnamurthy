#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include <android/native_window.h>
#include <android/native_window_jni.h>

using namespace std;
using namespace cv;

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "PoseEstimationMSA", __VA_ARGS__)

/*Vector to store the list of detected feature points from the reference image*/
vector<KeyPoint> detectedFeaturePoints;

/*Matrix to store the detected feature descriptors from the reference image*/
Mat detectedFeatureDescriptors;

/*Scaling parameter for the focal length. Increasing and decreasing the value will change the size of the cube*/
const double SCALE_PARAMETER = 2.0;

/*elevation value that will elevate the cube from the surface*/
const double ZELEVATION_VALUE = 25;

/*3d vector to store the detected feature points after conversion. It will be used in feature detection */
vector<Point3f>  detectedFeaturePoints3d;

/*Number of nearest points to be considered for KNN matcher*/
const int NUM_NEAREST_POINTS = 2;

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_asuforia_NativePoseEstimatorUtil_detect_1feature_1points(JNIEnv *env, jclass clazz, jstring reference_img_path, jint numFeaturePoints) {
    /* Get the path passed from the java method*/
    const char *ref_img_path = env->GetStringUTFChars(reference_img_path, 0);

    /*Number of feature points of interest passed from the library*/
    auto numRequestedFeatures = (unsigned int)numFeaturePoints;

    /*Reading the image from the given path in GRAYSCALE*/
    Mat referenceSurfaceImage = imread(ref_img_path,IMREAD_GRAYSCALE);

    /*Calculate the feature points using the ORB descriptor. Other that number of features, rest of the parameters are set to default as per the documentation*/
    Ptr<FeatureDetector> orbFeatureDetector = ORB::create(numRequestedFeatures,1.2f,8,31,0,2,ORB::HARRIS_SCORE,31,20);

    /*detect and compute the feature points and descriptors*/
    orbFeatureDetector->detect(referenceSurfaceImage, detectedFeaturePoints);
    orbFeatureDetector->compute(referenceSurfaceImage, detectedFeaturePoints, detectedFeatureDescriptors);

    /*Calculate the center of the image based on the image height and width*/
    const double centerCordinateX = (double)referenceSurfaceImage.cols/2;
    const double centerCordinateY = (double)referenceSurfaceImage.rows/2;

    /*Calculate the focal point co-ordinaties with certain scaling value*/
    const double focalCordinateX = centerCordinateX * SCALE_PARAMETER;
    const double focalCordinateY = centerCordinateY * SCALE_PARAMETER;

    for (auto & detectedFeaturePoint : detectedFeaturePoints) {
        /*
         * Converting the 2D image co-ordinate into 3D wordl co-ordinate following the equation
         * X = Z/focalX *  (u - centerX)
         * Y = Z/focalY *  (v - centerY)
         * u and v are 2D co-ordinates.
         * */
        auto X = (float) ((ZELEVATION_VALUE / focalCordinateX)*(detectedFeaturePoint.pt.x - centerCordinateX));
        auto Y = (float) ((ZELEVATION_VALUE / focalCordinateY)*(detectedFeaturePoint.pt.y - centerCordinateY));
        float Z = 0.0f;
        detectedFeaturePoints3d.emplace_back(X, Y, Z);
    }
    env->ReleaseStringUTFChars(reference_img_path, ref_img_path);
    return 0;
}

void symmetryTest( const std::vector<std::vector<cv::DMatch> >& matches1,
                                  const std::vector<std::vector<cv::DMatch> >& matches2,
                                  std::vector<cv::DMatch>& symMatches )
{

    // for all matches image 1 -> image 2
    for (std::vector<std::vector<cv::DMatch> >::const_iterator
                 matchIterator1 = matches1.begin(); matchIterator1 != matches1.end(); ++matchIterator1)
    {

        // ignore deleted matches
        if (matchIterator1->empty() || matchIterator1->size() < 2)
            continue;

        // for all matches image 2 -> image 1
        for (std::vector<std::vector<cv::DMatch> >::const_iterator
                     matchIterator2 = matches2.begin(); matchIterator2 != matches2.end(); ++matchIterator2)
        {
            // ignore deleted matches
            if (matchIterator2->empty() || matchIterator2->size() < 2)
                continue;

            // Match symmetry test
            if ((*matchIterator1)[0].queryIdx ==
                (*matchIterator2)[0].trainIdx &&
                (*matchIterator2)[0].queryIdx ==
                (*matchIterator1)[0].trainIdx)
            {
                // add symmetrical match
                symMatches.push_back(
                        cv::DMatch((*matchIterator1)[0].queryIdx,
                                   (*matchIterator1)[0].trainIdx,
                                   (*matchIterator1)[0].distance));
                break; // next match in image 1 -> image 2
            }
        }
    }

}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_asuforia_NativePoseEstimatorUtil_perform_1pose_1estimation(JNIEnv *env, jclass clazz, jobject image_buffer, jint width, jint height, jint numFeaturePoints) {

    auto *sourceImageBuffer = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(image_buffer));

    Mat grayScaleInputImg(height, width, CV_8UC1, sourceImageBuffer);

    /*Calculate the center of the image based on the image height and width*/
    const double centerCordinateX = (double)grayScaleInputImg.rows/2;
    const double centerCordinateY = (double)grayScaleInputImg.cols/2;

    /*Calculate the focal point co-ordinaties with certain scaling value*/
    const double focalCordinateX = centerCordinateX * SCALE_PARAMETER;
    const double focalCordinateY = centerCordinateY * SCALE_PARAMETER;

    /*Vector to store the list of detected feature points from the input image*/
    vector<KeyPoint> inputImgDetectedFeaturePoints;

    /*Matrix to store the detected feature descriptors from the input image*/
    Mat inputImgDetectedFeatureDescriptors;

    Ptr<FeatureDetector> detector = ORB::create(numFeaturePoints, 1.2f, 8, 31, 0, 2, ORB::HARRIS_SCORE, 31, 20);
    detector->detect(grayScaleInputImg, inputImgDetectedFeaturePoints);
    detector->compute(grayScaleInputImg, inputImgDetectedFeaturePoints, inputImgDetectedFeatureDescriptors);

    vector<vector<DMatch>> knnMatchedPoints;
    vector<DMatch> goodMatchedPoints;

    /*Instantiating Flann descriptor. The Index and Search Params are tuned for better execution time*/
    Ptr<flann::IndexParams> indexParams = makePtr<flann::LshIndexParams>(30, 8, 2);
    Ptr<flann::SearchParams> searchParams = makePtr<flann::SearchParams>(50);
    DescriptorMatcher * matcher = new FlannBasedMatcher(indexParams, searchParams);


//    /*Robust Match*/
//    std::vector<std::vector<cv::DMatch> > matches12, matches21;
//    matcher->knnMatch( inputImgDetectedFeatureDescriptors, detectedFeatureDescriptors,matches12,NUM_NEAREST_POINTS);
//    matcher->knnMatch( detectedFeatureDescriptors, inputImgDetectedFeatureDescriptors, matches21,NUM_NEAREST_POINTS);
//
//
//    symmetryTest(matches12, matches21, goodMatchedPoints);


    /*Fast Robust Match*/
    matcher->knnMatch( inputImgDetectedFeatureDescriptors, detectedFeatureDescriptors,knnMatchedPoints,NUM_NEAREST_POINTS);

    for (auto & knnMatchedPoint : knnMatchedPoints) {
        const DMatch &match1 = knnMatchedPoint[0];
        const DMatch &match2 = knnMatchedPoint[1];

        if (match1.distance < 0.8 * match2.distance)
            goodMatchedPoints.push_back(match1);
    }

    if(goodMatchedPoints.size() > 4){
        vector<Point2f> matchPoint1(goodMatchedPoints.size());
        vector<Point2f> matchPoint2(goodMatchedPoints.size());

        for(int i=0; i<goodMatchedPoints.size(); i++) {
            matchPoint1[i] = detectedFeaturePoints[goodMatchedPoints[i].trainIdx].pt;
            matchPoint2[i] = inputImgDetectedFeaturePoints[goodMatchedPoints[i].queryIdx].pt;
        }


        vector<unsigned char> inliersVector(matchPoint1.size());

        Mat homography = findHomography(matchPoint1, matchPoint2, FM_RANSAC, 5, inliersVector);


        vector<DMatch> inliers;
        for (size_t i = 0; i < inliersVector.size(); i++) {
            if (inliersVector[i])
                inliers.push_back(goodMatchedPoints[i]);
        }

        if(inliers.size() > 5) {
            vector<Point2f> input2DFeaturePoints;
            vector<Point3f> reference3DFeaturePoints;

            for (auto & inlier : inliers) {
                reference3DFeaturePoints.push_back(detectedFeaturePoints3d[inlier.trainIdx]);
                input2DFeaturePoints.push_back(
                        inputImgDetectedFeaturePoints[inlier.queryIdx].pt);
            }


            double cameraCoefficentVector[9] = {focalCordinateX, 0, centerCordinateX, 0,
                                                focalCordinateY, centerCordinateY, 0, 0, 1};

            Mat cameraCoefficentMatrix = Mat(3, 3, CV_64F, cameraCoefficentVector);

            Mat rotationVector, translationVector;

            bool isPoseFound = solvePnP(reference3DFeaturePoints, input2DFeaturePoints, cameraCoefficentMatrix,Mat::zeros(5, 1, CV_64F), rotationVector, translationVector);

            if(isPoseFound){
                float combinedRTVector[6];

                combinedRTVector[0] = float(rotationVector.at<double>(0,0));
                combinedRTVector[1] = float(rotationVector.at<double>(1,0));
                combinedRTVector[2] = float(rotationVector.at<double>(2,0));


                combinedRTVector[3] = float(translationVector.at<double>(0,0));
                combinedRTVector[4] = float(translationVector.at<double>(1,0));
                combinedRTVector[5] = float(translationVector.at<double>(2,0));

                jfloatArray outputVector = env->NewFloatArray(6);
                env->SetFloatArrayRegion( outputVector, 0, 6, &combinedRTVector[0] );

                /*Release memory*/
                cameraCoefficentMatrix.release();
                rotationVector.release();
                translationVector.release();
                inputImgDetectedFeaturePoints.clear();
                reference3DFeaturePoints.clear();
                inliers.clear();
                inliersVector.clear();
                homography.release();
                inputImgDetectedFeatureDescriptors.release();
                matchPoint1.clear();
                matchPoint2.clear();

                return outputVector;
            }

            /*Release memory*/
            cameraCoefficentMatrix.release();
            rotationVector.release();
            translationVector.release();
            inputImgDetectedFeaturePoints.clear();
            reference3DFeaturePoints.clear();
            inliers.clear();
            inliersVector.clear();
            homography.release();
            inputImgDetectedFeatureDescriptors.release();
            matchPoint1.clear();
            matchPoint2.clear();
        }
    }

    return nullptr;
}
