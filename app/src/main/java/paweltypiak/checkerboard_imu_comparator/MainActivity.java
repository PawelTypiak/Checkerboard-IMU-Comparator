package paweltypiak.checkerboard_imu_comparator;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.opencv.android.OpenCVLoader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private SensorInitializer sensorInitializer;
    private CameraInitializer cameraInitializer;
    private CheckerboardPatternComputingInitializer checkerboardPatternComputingInitializer;
    private int necessaryImagesNumber;
    private int checkerboardWidth;
    private int checkerboardHeight;
    private double xSensorAxis;
    private double ySensorAxis;
    private SharedPreferences sharedPreferences;
    private Button calibrateButton;
    private Button compareButton;
    private TextView numberOfImagesTextView;
    private TextView isCalibrationDoneTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sensorInitializer = new SensorInitializer(this);
        cameraInitializer = new CameraInitializer(this);
        initializeOpenCVLibrary();
        initializeCheckerboardPatternComputing();
        initializeNumberOfImagesText();
        initializeCalibrateButton();
        initializeCompareButton();
        initializeCaptureButton();
        initializeSettingsButton();
        updateLayoutOnCalibrationResult();
    }

    private void getSettings() {
        sharedPreferences = getSharedPreferences(getString(R.string.sharedprefrences_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        getNecessaryImagesNumber(editor);
        getCheckerboardWidth(editor);
        getCheckerboardHeight(editor);
        editor.apply();
    }

    private void getNecessaryImagesNumber(SharedPreferences.Editor editor){
        String key=getString(R.string.sharedpreferences_necessary_images_number_key);
        if (sharedPreferences.getInt(key, 0) == 0) {
            final int DEFAULT_NECESSARY_IMAGES_NUMBER=15;
            editor.putInt(key, DEFAULT_NECESSARY_IMAGES_NUMBER);
            this.necessaryImagesNumber = DEFAULT_NECESSARY_IMAGES_NUMBER;
        } else {
            this.necessaryImagesNumber = sharedPreferences.getInt(key, 0);
        }
    }

    private void getCheckerboardWidth(SharedPreferences.Editor editor){
        String key=getString(R.string.sharedpreferences_checkerboard_width_key);
        if (sharedPreferences.getInt(key, 0) == 0) {
            final int DEFAULT_CHECKERBOARD_WIDTH=9;
            editor.putInt(key, DEFAULT_CHECKERBOARD_WIDTH);
            this.checkerboardWidth = DEFAULT_CHECKERBOARD_WIDTH;
        } else {
            this.checkerboardWidth = sharedPreferences.getInt(key, 0);
        }
    }

    private void getCheckerboardHeight(SharedPreferences.Editor editor){
        String key=getString(R.string.sharedpreferences_checkerboard_height_key);
        if (sharedPreferences.getInt(key, 0) == 0) {
            final int DEFAULT_CHECKERBOARD_HEIGHT=6;
            editor.putInt(key, DEFAULT_CHECKERBOARD_HEIGHT);
            this.checkerboardHeight = DEFAULT_CHECKERBOARD_HEIGHT;
        } else {
            this.checkerboardHeight = sharedPreferences.getInt(key, 0);
        }
    }

    private void initializeOpenCVLibrary() {
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }
    }

    private void initializeCheckerboardPatternComputing(){
        getSettings();
        checkerboardPatternComputingInitializer
                = new CheckerboardPatternComputingInitializer(
                this,
                checkerboardWidth,
                checkerboardHeight,
                necessaryImagesNumber);
    }

    private void initializeNumberOfImagesText(){
        numberOfImagesTextView = (TextView) findViewById(R.id.images_number_text_view);
        numberOfImagesTextView.setText(Integer.toString(getNumberOfSavedImages()));
    }

    private void initializeCalibrateButton() {
        calibrateButton = (Button) findViewById(R.id.calibrate_button);
        calibrateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getNumberOfSavedImages() < necessaryImagesNumber) {
                    Toast.makeText(MainActivity.this.getApplicationContext(),
                            getString(R.string.not_enought_images_to_calibration_toast_message) + necessaryImagesNumber,
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (isCalibrationDone() == false) {
                        new CalibrateCameraTask().execute();
                    } else {
                        Toast.makeText(MainActivity.this.getApplicationContext(),
                                getString(R.string.calibration_already_done_toast_message), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private class CalibrateCameraTask extends AsyncTask<Void, Void, Void> {

        private ProgressDialog calibrationProgressDialog= showCalibrationProgressDialog();

        protected Void doInBackground(Void... voids) {
            checkerboardPatternComputingInitializer.startCalibration();
            return null;
        }

        protected void onPostExecute(Void result) {
            updateLayoutOnCalibrationResult();
            calibrationProgressDialog.dismiss();
            showCalibrationResultDialog();
        }
    }

    private ProgressDialog showCalibrationProgressDialog(){
        final ProgressDialog calibrationProgressDialog = ProgressDialog.show(
                MainActivity.this,
                getString(R.string.calibration_progress_dialog_title),
                getString(R.string.calibration_progress_dialog_message), true);
        calibrationProgressDialog.setCancelable(false);
        return calibrationProgressDialog;
    }

    private void showCalibrationResultDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle(getString(R.string.calibration_results_dialog_title));
        alert.setMessage(getString(
                R.string.calibration_results_dialog_message) +" "+
                checkerboardPatternComputingInitializer.getRejectedImage() +
                "/"
                + checkerboardPatternComputingInitializer.getNecessaryImagesNumber());
        alert.setPositiveButton(getString(R.string.calibration_results_dialog_positive_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = alert.create();
        alertDialog.show();
    }

    private void initializeCompareButton() {
        compareButton = (Button) findViewById(R.id.compare_button);
        compareButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCalibrationDone()) {
                   new CompareImuToCheckerboardTask().execute();
                }
            }
        });
    }

    private class CompareImuToCheckerboardTask extends AsyncTask<Void, Void, Boolean> {

        private ProgressDialog comparationProgressDialog= showComparationProgressDialog();

        protected Boolean doInBackground(Void... voids) {
            int numberOfSavedImages=getNumberOfSavedImages();
            String picName = getString(R.string.image_name) + Integer.toString(numberOfSavedImages-1) + ".jpg";
            Boolean isChesseboardFound
                    = checkerboardPatternComputingInitializer.computeAngleFromCheckerboard(
                    checkerboardWidth,
                    checkerboardHeight,
                    picName);
            return isChesseboardFound;
        }

        protected void onPostExecute(Boolean result) {
            comparationProgressDialog.dismiss();
            if(result==true){
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                saveResultsToFile(decimalFormat);
                showComparationResultsDialog(decimalFormat);
            }
            else {
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.no_checkerboard_detected_toast_message),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private ProgressDialog showComparationProgressDialog(){
        final ProgressDialog comparationProgressDialog = ProgressDialog.show(
                this,
                getString(R.string.comparation_progress_dialog_title),
                getString(R.string.comparation_progress_dialog_message)
        );
        comparationProgressDialog.setCancelable(false);
        return comparationProgressDialog;
    }

    private void saveResultsToFile(DecimalFormat decimalFormat){
        File mediaStorageDir = new File("/sdcard/", getString(R.string.app_name));
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + getString(R.string.results_file_name));
        try {
            FileWriter fileWriter = new FileWriter(mediaFile, true);
            fileWriter.append("\n\nIMG_" + Integer.toString(getNumberOfSavedImages()-1) + " |sexsorX|sensorY|opencvX|opoencvY|:");
            fileWriter.append("\n" + decimalFormat.format(xSensorAxis)
                    + " " + decimalFormat.format(ySensorAxis)
                    + " " + decimalFormat.format(checkerboardPatternComputingInitializer.getxAxis())
                    + " " + decimalFormat.format(checkerboardPatternComputingInitializer.getyAxis()));
            fileWriter.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    private void showComparationResultsDialog(DecimalFormat decimalFormat){
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle(getString(R.string.comparation_results_dialog_title));
        alert.setMessage("IMU:" +
                "\n   X = " + decimalFormat.format(xSensorAxis) +
                "\n   Y = " + decimalFormat.format(ySensorAxis) +
                "\nCamera:" +
                "\n   X = " + decimalFormat.format(checkerboardPatternComputingInitializer.getxAxis()) +
                "\n   Y = " + decimalFormat.format(checkerboardPatternComputingInitializer.getyAxis()));
        alert.setPositiveButton(getString(R.string.comparation_results_dialog_positive_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog comparationResultsDialog = alert.create();
        comparationResultsDialog.show();
    }

    private void initializeCaptureButton() {
        Button captureButton = (Button) findViewById(R.id.capture_button);
        captureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean trySavePicture=savePicture();
                if(trySavePicture==true){
                    getSensorsData();
                    int numberOfImages = getNumberOfSavedImages();
                    cameraInitializer.setCurrentImagesNumber(numberOfImages);
                    numberOfImagesTextView.setText(Integer.toString(numberOfImages+1));
                    Toast.makeText(MainActivity.this.getApplicationContext(),
                            getString(R.string.image_saved_toast_message), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getSensorsData(){
        SensorManager.getRotationMatrix(sensorInitializer.getRotationMatrix(), null,
                sensorInitializer.getAccelerationValues(), sensorInitializer.getMagneticValues());
        SensorManager.getOrientation(sensorInitializer.getRotationMatrix(), sensorInitializer.getOrientationValues());
        xSensorAxis = sensorInitializer.getOrientationValues()[1] * 57.3f * (-1);
        ySensorAxis = sensorInitializer.getOrientationValues()[2] * 57.3f + 90.0f;
    }

    private boolean savePicture(){
        Camera.PictureCallback picture=cameraInitializer.getPicture();
        if(picture!=null){
            cameraInitializer.getCamera().takePicture(null, null, cameraInitializer.getPicture());
            return true;
        }
        else{
            return false;
        }
    }

    private void initializeSettingsButton() {
        Button settingsButton = (Button) findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(i, 0);
            }
        });
    }

    private int getNumberOfSavedImages() {
        int numberOfSavedImages = 0;
        File dir = new File(getString(R.string.images_directory));
        if (dir.isDirectory()) {
            File[] savedImagesArray;
            savedImagesArray = dir.listFiles();
            for (File i: savedImagesArray) {
                if (i.getName().endsWith(".jpg")){
                    numberOfSavedImages++;
                }
            }
        }
        return numberOfSavedImages;
    }

    private boolean updateLayoutOnCalibrationResult(){
        if(isCalibrationDoneTextView == null){
            isCalibrationDoneTextView = (TextView) findViewById(R.id.is_calibrated_text_view);
        }
        if (isCalibrationDone()){
            isCalibrationDoneTextView.setText(getString(R.string.is_calibrated_positive));
            calibrateButton.setEnabled(false);
            calibrateButton.setTextColor(ContextCompat.getColor(this,R.color.textDisabledDarkBackground));
            compareButton.setEnabled(true);
            compareButton.setTextColor(ContextCompat.getColor(this,R.color.textDarkBackground));
            return true;
        } else {
            isCalibrationDoneTextView.setText(getString(R.string.is_calibrated_negative));
            calibrateButton.setEnabled(true);
            calibrateButton.setTextColor(ContextCompat.getColor(this,R.color.textDarkBackground));
            compareButton.setEnabled(false);
            compareButton.setTextColor(ContextCompat.getColor(this,R.color.textDisabledDarkBackground));
            return false;
        }
    }

    private boolean isCalibrationDone(){
        if (sharedPreferences.getBoolean(getString(R.string.sharedpreferences_is_calibration_done_key), false) == true){
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                boolean result = data.getBooleanExtra(getString(R.string.settings_is_restore_clicked_key), false);
                if (result) {
                    restoreDefaults();
                }
            }
            else{
                updateLayoutOnCalibrationResult();
            }
            getSettings();
        }
    }

    private void restoreDefaults() {
        File file = new File(getString(R.string.images_directory));
        if (file.exists() && file.isDirectory()) {
            clearDirectory(file);
            SharedPreferences.Editor editor;
            editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.sharedpreferences_is_calibration_done_key), false);
            editor.apply();
            updateLayoutOnCalibrationResult();
        }
    }

    private void clearDirectory(File file){
        String[] children = file.list();
        for (int i = 0; i < children.length; i++) {
            new File(file, children[i]).delete();
        }
        cameraInitializer.setCurrentImagesNumber(0);
        numberOfImagesTextView.setText("0");
        Toast.makeText(MainActivity.this.getApplicationContext(),
                getString(R.string.directory_clear_toast_message),
                Toast.LENGTH_SHORT).show();
    }

    public void onResume() {
        super.onResume();
        if (cameraInitializer.onResume() == false){
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraInitializer.releaseCamera();
    }
}
