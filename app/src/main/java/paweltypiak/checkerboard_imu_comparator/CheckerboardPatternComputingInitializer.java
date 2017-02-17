package paweltypiak.checkerboard_imu_comparator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CheckerboardPatternComputingInitializer {

    private final String TAG = "CheckerboardComputing::";
    private int width;
    private int height;
    private ArrayList<Mat> images;
    private MatOfPoint2f corners;
    private ArrayList<Mat> cornersFromAllImages;
    private MatOfPoint3f cornersModel;
    private ArrayList<Mat> cornersModelFromAllImages;
    private Mat cameraMatrix;
    private Mat distCoeff;
    private ArrayList<Mat> rvecs;
    private ArrayList<Mat> tvecs;
    private final double scaleSize = 0.25;
    private Activity activity;
    private int necessaryImagesNumber;
    private int rejectedImage;
    private Mat IMAGE;
    private MatOfPoint2f cornerOfOneImage;
    private MatOfPoint2f externalCorners;
    private MatOfPoint3f externalCornersModel;
    private double zAxis;
    private double yAxis;
    private double xAxis;

    public CheckerboardPatternComputingInitializer(Activity activity, int width, int height, int necessaryImagesNumber) {
        this.activity = activity;
        this.width = width;
        this.height = height;
        this.necessaryImagesNumber = necessaryImagesNumber;
        images = new ArrayList<>();
    }

    public void startCalibration() {
        rejectedImage = 0;
        readImagesToMat();
        calibrateCamera();
    }

    private void readImagesToMat() {
        String mediaStorageDir = activity.getString(R.string.images_directory);
        File mediaStorageDirFiles = new File("/sdcard/", activity.getString(R.string.app_name));
        if (mediaStorageDirFiles.exists()){
            ArrayList<String> mediaFile = new ArrayList<>();
            for (int i = 0; i < necessaryImagesNumber; i++){
                mediaFile.add(mediaStorageDir + "/" + "IMG_" + i + ".jpg");
            }
            for (String element: mediaFile) {
                Log.d(TAG, "Loading file - " + element);
                images.add(Imgcodecs.imread(element));
            }
            Log.d(TAG, "Images loaded");
        }
    }

    private void calibrateCamera() {
        resizeAndConvertImages();
        findCheckereboardPatterns();
        createModelOfPattern();
        makeCalibrationOfPatterns();
        saveCameraPreferences();
        writeThatThisConfigFileExist();
    }

    private void resizeAndConvertImages() {
        for (int i = 0; i < images.size(); i++) {
            Mat imgTMP = new Mat();
            Size size = new Size(images.get(i).cols() * scaleSize, images.get(i).rows() * scaleSize);
            Imgproc.resize(images.get(i), imgTMP, size);
            images.set(i, imgTMP);
            Imgproc.cvtColor(images.get(i), imgTMP, Imgproc.COLOR_BGR2GRAY);
            images.set(i, imgTMP);
        }
    }

    private void findCheckereboardPatterns() {
        cornersFromAllImages = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            corners = new MatOfPoint2f();
            boolean patternFound = Calib3d.findCheckerboardCorners(images.get(i),
                    new Size(width, height), corners, Calib3d.CALIB_CB_FAST_CHECK);
            if (patternFound) {
                TermCriteria termCriteria = new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS,
                        30, 0.1);
                Imgproc.cornerSubPix(images.get(i), corners, new Size(width, height),
                        new Size(-1, -1), termCriteria);
                cornersFromAllImages.add(corners);
                Log.d(TAG, "IMG_" + String.valueOf(i + 1) + " -> PATTERN FOUND");
            } else {
                rejectedImage++;
                Log.d(TAG, "IMG_" + String.valueOf(i + 1) + " -> PATTERN NOT FOUND");
            }
        }
    }

    private void createModelOfPattern() {
        List<Point3> matTMP = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Point3 point3 = new Point3(i, j, 0);
                matTMP.add(point3);
            }
        }
        cornersModel = new MatOfPoint3f();
        cornersModel.fromList(matTMP);
        cornersModelFromAllImages = new ArrayList<>();
        for (int i = 0 ; i < cornersFromAllImages.size() ; i++){
            cornersModelFromAllImages.add(cornersModel);
        }
    }

    private void makeCalibrationOfPatterns() {
        cameraMatrix = new Mat();
        distCoeff = new Mat();
        rvecs = new ArrayList<>();
        tvecs = new ArrayList<>();
        Calib3d.calibrateCamera(cornersModelFromAllImages, cornersFromAllImages,
                images.get(0).size(), cameraMatrix, distCoeff, rvecs, tvecs);
    }

    private void saveCameraPreferences() {
        CalibrationResultInitializer.save(activity, cameraMatrix, distCoeff);
    }

    private void writeThatThisConfigFileExist() {
        SharedPreferences sharedPreferences;
        sharedPreferences = activity.getSharedPreferences(activity.getString(R.string.sharedprefrences_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor;
        editor = sharedPreferences.edit();
        editor.putBoolean(activity.getString(R.string.sharedpreferences_is_calibration_done_key), true);
        editor.apply();
    }

    public boolean computeAngleFromCheckerboard(int width, int height , String picName) {
        this.width = width;
        this.height = height;
        readCameraParameters();
        Mat image = Imgcodecs.imread(picName);
        IMAGE = new Mat();
        this.IMAGE = image.clone();
        this.cornerOfOneImage = new MatOfPoint2f();
        resizeAndConvertOneImage();
        if (findPattern()){
            createExternalCornersModel();
            computeRotateAngles();
            return true;
        } else {

            return false;
        }
    }

    private void readCameraParameters() {
        Mat camM = new Mat(3, 3, CvType.CV_64FC1);
        Mat difC = new Mat(1, 5, CvType.CV_64FC1);
        CalibrationResultInitializer.tryLoad(activity, camM, difC);
        this.cameraMatrix = camM.clone();
        this.distCoeff = difC.clone();
    }

    private void resizeAndConvertOneImage() {
        Size size = new Size(IMAGE.cols() * scaleSize, IMAGE.rows() * scaleSize);
        Mat imgTMP = new Mat();
        Imgproc.resize(IMAGE, imgTMP, size);
        IMAGE = imgTMP.clone();
        Imgproc.cvtColor(IMAGE, imgTMP, Imgproc.COLOR_BGR2GRAY);
        IMAGE = imgTMP.clone();
    }

    private boolean findPattern() {
        boolean isPatternFound = Calib3d.findCheckerboardCorners(IMAGE, new Size(width, height),
                cornerOfOneImage, Calib3d.CALIB_CB_FAST_CHECK);
        if (isPatternFound) {
            TermCriteria termCriteria = new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS,
                    30, 0.1);
            Imgproc.cornerSubPix(IMAGE, cornerOfOneImage, new Size(width, height),
                    new Size(-1, -1), termCriteria);
            Log.d(TAG, "findPattern(picture, externalCorners) -> done - found checkerboard");
            return true;
        } else {
            Log.d(TAG, "findPattern(picture, externalCorners) -> done - no checkerboard detected");
            return false;
        }
    }

    private void createExternalCornersModel() {
        this.externalCorners = new MatOfPoint2f(
                new Point(cornerOfOneImage.get(cornerOfOneImage.rows() - width, 0)[0], cornerOfOneImage.get(cornerOfOneImage.rows() - width, 0)[1]),
                new Point(cornerOfOneImage.get(0, 0)[0], cornerOfOneImage.get(0, 0)[1]),
                new Point(cornerOfOneImage.get(width - 1, 0)[0], cornerOfOneImage.get(width - 1, 0)[1]),
                new Point(cornerOfOneImage.get(cornerOfOneImage.rows() - 1, 0)[0], cornerOfOneImage.get(cornerOfOneImage.rows() - 1, 0)[1]));
        this.externalCornersModel = new MatOfPoint3f(
                new Point3(0, (height - 1)*100, 0),
                new Point3(0, 0, 0),
                new Point3((width - 1)*100, 0, 0),
                new Point3((width - 1)*100, (height - 1)*100, 0));
    }

    private void computeRotateAngles() {
        Mat rot = new Mat();
        Mat trans = new Mat();
        MatOfDouble distCoeffDouble = new MatOfDouble(distCoeff);
        boolean solver = Calib3d.solvePnP(externalCornersModel, externalCorners, cameraMatrix,
                distCoeffDouble, rot, trans);
        Mat rotM = new Mat();
        Calib3d.Rodrigues(rot, rotM);
        rotM = rotM.t();
        Mat zero = Mat.zeros(3, 1, CvType.CV_64FC1);
        Mat projMatrix = new Mat();
        List<Mat> matList = new ArrayList<>();
        matList.add(rotM);
        matList.add(zero);
        Core.hconcat(matList, projMatrix);
        MatOfDouble cameraMatrix = new MatOfDouble(3, 3, CvType.CV_32F);
        MatOfDouble rotMatrix = new MatOfDouble(3, 3, CvType.CV_32F);
        MatOfDouble transVect = new MatOfDouble(4, 1, CvType.CV_32F);
        MatOfDouble rotMatrixX = new MatOfDouble(3, 3, CvType.CV_32F);
        MatOfDouble rotMatrixY = new MatOfDouble(3, 3, CvType.CV_32F);
        MatOfDouble rotMatrixZ = new MatOfDouble(3, 3, CvType.CV_32F);
        MatOfDouble eulerAngles = new MatOfDouble(3, 1, CvType.CV_32F);
        Calib3d.decomposeProjectionMatrix(projMatrix,
                cameraMatrix,
                rotMatrix,
                transVect,
                rotMatrixX,
                rotMatrixY,
                rotMatrixZ,
                eulerAngles);
        this.yAxis = eulerAngles.get(0, 0)[0] * (-1);
        this.zAxis = eulerAngles.get(1, 0)[0];
        this.xAxis = eulerAngles.get(2, 0)[0];
        if (xAxis < -90) {
            this.xAxis += 180;
        } else if (xAxis > 90) {
            this.xAxis -= 180;
        }
        Log.d(TAG, "yAxis= " + yAxis);
        Log.d(TAG, "xAxis= " + xAxis);
    }

    public double getxAxis() {
        return xAxis;
    }

    public double getyAxis() {
        return yAxis;
    }

    public int getNecessaryImagesNumber() {
        return necessaryImagesNumber;
    }

    public int getRejectedImage() {
        return rejectedImage;
    }
}
