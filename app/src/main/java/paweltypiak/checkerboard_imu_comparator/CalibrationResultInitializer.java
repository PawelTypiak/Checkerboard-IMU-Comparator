package paweltypiak.checkerboard_imu_comparator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.opencv.core.Mat;

public class CalibrationResultInitializer {

    private static final String TAG = "CalibrationInitializer:";
    private static final int CAMERA_MATRIX_ROWS = 3;
    private static final int CAMERA_MATRIX_COLS = 3;
    private static final int DISTORTION_COEFFICIENTS_SIZE = 5;

    public static void save(Activity activity, Mat cameraMatrix, Mat distCoeff) {
        SharedPreferences sharedPref = activity.getSharedPreferences(activity.getString(R.string.sharedprefrences_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        double[] cameraMatrixArray = new double[CAMERA_MATRIX_ROWS * CAMERA_MATRIX_COLS];
        cameraMatrix.get(0,  0, cameraMatrixArray);
        for (int i = 0; i < CAMERA_MATRIX_ROWS; i++) {
            for (int j = 0; j < CAMERA_MATRIX_COLS; j++) {
                Integer id = i * CAMERA_MATRIX_ROWS + j;
                editor.putFloat(activity.getString(R.string.sharedpreferences_camera_matrix_key) + id.toString(), (float)cameraMatrixArray[id]);
            }
        }
        double[] distortionCoefficientsArray = new double[DISTORTION_COEFFICIENTS_SIZE];
        distCoeff.get(0, 0, distortionCoefficientsArray);
        for (Integer i = 0; i < DISTORTION_COEFFICIENTS_SIZE ; i++) {
            editor.putFloat(activity.getString(R.string.sharedpreferences_distortion_coefficients_key) + i.toString(), (float)distortionCoefficientsArray[i]);
        }
        editor.commit();
        Log.d(TAG, "Saved camera matrix: " + cameraMatrix.dump());
        Log.d(TAG, "Saved distortion coefficients: " + distCoeff.dump());
    }

    public static boolean tryLoad(Activity activity, Mat cameraMatrix, Mat distCoeff) {
        SharedPreferences sharedPref = activity.getSharedPreferences(activity.getString(R.string.sharedprefrences_key), Context.MODE_PRIVATE);
        if (sharedPref.getFloat(activity.getString(R.string.sharedpreferences_camera_matrix_key) + 0, -1) == -1) {
            Log.d(TAG, "No previous calibration results found");
            return false;
        }
        double[] cameraMatrixArray = new double[CAMERA_MATRIX_ROWS * CAMERA_MATRIX_COLS];
        for (int i = 0; i < CAMERA_MATRIX_ROWS; i++) {
            for (int j = 0; j < CAMERA_MATRIX_COLS; j++) {
                Integer id = i * CAMERA_MATRIX_ROWS + j;
                cameraMatrixArray[id] = sharedPref.getFloat(activity.getString(R.string.sharedpreferences_camera_matrix_key) + id.toString(), -1);
            }
        }
        cameraMatrix.put(0, 0, cameraMatrixArray);
        Log.d(TAG, "Loaded camera matrix: " + cameraMatrix.dump());
        double[] distortionCoefficientsArray = new double[DISTORTION_COEFFICIENTS_SIZE];
        for (Integer i = 0; i < DISTORTION_COEFFICIENTS_SIZE ; i++) {
            distortionCoefficientsArray[i] = sharedPref.getFloat(activity.getString(R.string.sharedpreferences_distortion_coefficients_key) + i.toString(), -1);
        }
        distCoeff.put(0, 0, distortionCoefficientsArray);
        Log.d(TAG, "Loaded distortion coefficients: " + distCoeff.dump());
        return true;
    }
}
