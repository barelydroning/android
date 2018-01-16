package com.barelydroning.drone;

import com.google.gson.annotations.Expose;

/**
 * Created by andreas on 2017-11-19.
 */

public class Data {

    @Expose private float pitch;
    @Expose private float roll;
    @Expose private float azimuth;
    @Expose private float altitude;

    public Data(float pitch, float roll, float azimuth, float altitude) {
        this.pitch = pitch;
        this.roll = roll;
        this.azimuth = azimuth;
        this.altitude = altitude;
    }
}
