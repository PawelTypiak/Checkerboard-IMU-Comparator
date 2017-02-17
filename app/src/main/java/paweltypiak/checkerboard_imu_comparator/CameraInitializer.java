package paweltypiak.checkerboard_imu_comparator;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.widget.FrameLayout;
import android.widget.Toast;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraInitializer {

    private Activity activity;
    private Camera camera;
    private CameraPreview cameraPreview;
    private Camera.PictureCallback Picture;
    private int currentImagesNumber;

    public CameraInitializer(Activity activity) {
        this.activity = activity;
        addCameraPreviewView();
    }

    public void addCameraPreviewView(){
        FrameLayout cameraPreviewLayout = (FrameLayout) activity.findViewById(R.id.camera_layout);
        cameraPreview = new CameraPreview(activity, camera);
        cameraPreviewLayout.addView(cameraPreview);
    }

    public int findBackFacingCamera() {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    public void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    public boolean onResume() {
        if (!hasCamera(activity)) {
            Toast.makeText(activity,
                    activity.getString(R.string.camera_unavailable_toast_message),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        if (camera == null) {
            camera = Camera.open(findBackFacingCamera());
            Picture = getPictureCallback();
            cameraPreview.refreshCamera(camera);
        }
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, activity, mLoaderCallback);
        return true;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(activity) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS ) {} else {
                super.onManagerConnected(status);
            }
        }
    };

    private Camera.PictureCallback getPictureCallback() {
        Camera.PictureCallback picture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                File pictureFile = getOutputMediaFile(currentImagesNumber);
                if (pictureFile == null) {
                    return;
                }
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();

                } catch (FileNotFoundException e) {
                } catch (IOException e) {}
                cameraPreview.refreshCamera(CameraInitializer.this.camera);
            }
        };
        return picture;
    }

    private File getOutputMediaFile(int picNr) {
        File mediaStorageDir = new File("/sdcard/",activity.getString(R.string.app_name) );
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + picNr + ".jpg");
        return mediaFile;
    }

    private boolean hasCamera(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    public Camera getCamera() {
        return camera;
    }

    public Camera.PictureCallback getPicture() {
        return Picture;
    }

    public void setCurrentImagesNumber(int currentImagesNumber) {
        this.currentImagesNumber = currentImagesNumber;
    }
}
