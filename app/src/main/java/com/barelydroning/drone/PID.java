package com.barelydroning.drone;

/**
 * Created by Erik Marcus on 2017-11-15
 */
public class PID {

    //
    //  Parameters
    //
    private double[] parameters;
    private double maxIntegral;
    private double[] outputLimits;

    //
    // Variables
    //
    private double integral;
    private double lastSensorSignal;
    private double lastTargetValue;
    private double lastError;
    private long lastCallTime = 0;


    private double constrain(double value) {
        return Math.max(Math.min(value, outputLimits[1]), outputLimits[0]);
    }

    public PID(double kp, double ki, double kd) {
        setParameters(kp,ki,kd);
        setOutputLimits(-1000, 1000);
        setIntegralLimit((outputLimits[1] - outputLimits[0])/kd);
    }

    public void setParameters(double kp, double ki, double kd) {
        parameters = new double[] {kp, ki, kd};
    }

    public void setOutputLimits(double lowerBound, double upperBound) {
        outputLimits = new double[] {lowerBound, upperBound};
    }

    /**
     * It is recommended calling this function after each new value un ki.
     * @param maxIntegral
     */
    public void setIntegralLimit(double maxIntegral) {
        this.maxIntegral = maxIntegral;
    }

    public double calculate(double targetValue, double filteredSensorValue) {
        long newTime = System.nanoTime();

        if (lastCallTime == 0) {
            lastCallTime = newTime;
            lastSensorSignal = filteredSensorValue;
            lastTargetValue = targetValue;
            lastError = 0;
            integral = 0;

            return 0;
        }

        double deltaTime = (newTime - lastCallTime) / 1e9;
        deltaTime = 1;
        double error = targetValue - filteredSensorValue;

        // Integral
        integral += 0.5 * (error - lastError) * deltaTime;
        if (integral > maxIntegral) integral = maxIntegral;
        //if (lastTargetValue != targetValue) integral = 0;

        // Derivative
        double derivative = (lastTargetValue == targetValue) ? (error - lastError)/deltaTime : 0;

        return parameters[0]*error + parameters[1]*integral + parameters[2]*derivative;
    }

}