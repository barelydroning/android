package com.barelydroning.drone;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.WindowManager;

/**
 * Created by andreas on 2017-07-08.
 */

public abstract class OrientationManager implements SensorEventListener {
    private static final String TAG = OrientationManager.class.getSimpleName();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private WindowManager windowManager;
    private Sensor rotationVectorSensor;

    public OrientationManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        //accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        Sensor magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (magneticFieldSensor == null) {
            Log.i(TAG, "Magnetic field sensor is null");
        } else {
            Log.i(TAG, "Magnetic field sensor exists!");
        }

        if (accelerometerSensor == null) {
            Log.i(TAG, "Accelerometer sensor is null");
        } else {
            Log.i(TAG, "Accelerometer sensor exists!");
        }

        if (gyroscopeSensor == null) {
            Log.i(TAG, "Gyroscope sensor is null");
        } else {
            Log.i(TAG, "Gyroscope sensor exists!");
        }

        if (rotationVectorSensor == null) {
            Log.i(TAG, "Rotation vector sensor is null");
        } else {
            Log.i(TAG, "Rotation vector sensor exists!");
        }
    }

    public void register() {
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void unregister() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //float[] values = event.values;

        float[] rotationVector = new float[16];
        SensorManager.getRotationMatrixFromVector(rotationVector, event.values);

        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationVector, orientation);

//        sensorManager.getRotationMatrix()
//
//        int displayRotation = windowManager.getDefaultDisplay().getRotation();
//
//        float[] values = adjustAccelOrientation(displayRotation, event.values);

//        float azimuth = toDegrees(orientation[0]);
//        float pitch = toDegrees(orientation[1]);
//        float roll = toDegrees(orientation[2]);

        float azimuth = orientation[0];
        float pitch = orientation[1];
        float roll = orientation[2];

        sensorValues(azimuth, pitch, roll);

        //System.out.println("X: " + orientation[0] + " Y: " + orientation[1] + " Z: " + orientation[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private float toDegrees(float radians) {
        return (float) (360 * radians / (Math.PI));
    }

    public abstract void sensorValues(float azimuth, float pitch, float roll);

    private float[] adjustAccelOrientation(int displayRotation, float[] eventValues)
    {
        float[] adjustedValues = new float[3];

        final int axisSwap[][] = {
                {  1,  -1,  0,  1  },     // ROTATION_0
                {-1,  -1,  1,  0  },     // ROTATION_90
                {-1,    1,  0,  1  },     // ROTATION_180
                {  1,    1,  1,  0  }  }; // ROTATION_270

        final int[] as = axisSwap[displayRotation];
        adjustedValues[0]  =  (float)as[0] * eventValues[ as[2] ];
        adjustedValues[1]  =  (float)as[1] * eventValues[ as[3] ];
        adjustedValues[2]  =  eventValues[2];

        return adjustedValues;
    }
}
