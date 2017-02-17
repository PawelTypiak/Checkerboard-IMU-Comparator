package paweltypiak.checkerboard_imu_comparator;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorInitializer {

    private float[] magneticValues;
    private float[] accelerationValues;
    private float[] orientationValues;
    private float[] rotationMatrix;

    public SensorInitializer(Context context) {
        initializeSensor(context);
    }

    public void initializeSensor(Context context){
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        magneticValues = new float[3];
        accelerationValues = new float[3];
        orientationValues = new float[3];
        rotationMatrix = new float[9];
        final SensorEventListener mEventListener = new SensorEventListener() {
            public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}
            public void onSensorChanged(SensorEvent event) {
                switch (event.sensor.getType()) {
                    case android.hardware.Sensor.TYPE_ACCELEROMETER:
                        System.arraycopy(event.values, 0, accelerationValues, 0, 3);
                        break;
                    case android.hardware.Sensor.TYPE_MAGNETIC_FIELD:
                        System.arraycopy(event.values, 0, magneticValues, 0, 3);
                        break;
                }
            }
        };
        setListeners(sensorManager, mEventListener);
    }

    public void setListeners(SensorManager sensorManager, SensorEventListener mEventListener)
    {
        sensorManager.registerListener(mEventListener,
                sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(mEventListener,
                sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public float[] getMagneticValues() {
        return magneticValues;
    }

    public float[] getAccelerationValues() {
        return accelerationValues;
    }

    public float[] getOrientationValues() {
        return orientationValues;
    }

    public float[] getRotationMatrix() {
        return rotationMatrix;
    }
}
