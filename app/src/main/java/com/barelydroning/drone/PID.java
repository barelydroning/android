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
    private double lastTargetValue;
    private double lastError;
    private long lastCallTime = 0;

    // last output
    private double[] lastOutput;


    private double constrain(double value) {
        return Math.max(Math.min(value, outputLimits[1]), outputLimits[0]);
    }


    public PID(double kp, double ki, double kd, int baseSpeed) {
        setParameters(kp, ki, kd);
        int diff = Math.min(baseSpeed - 1000, 2000 - baseSpeed);
        setOutputLimits(-diff, diff);
//        setIntegralLimit((outputLimits[1] - outputLimits[0])/ki);
        setIntegralLimit(3);
    }

    public void setParameters(double kp, double ki, double kd) {
        parameters = new double[]{kp, ki, kd};
    }

    public void setOutputLimits(double lowerBound, double upperBound) {
        outputLimits = new double[]{lowerBound, upperBound};
    }

    /**
     * It is recommended calling this function after each new value un ki.
     *
     * @param maxIntegral
     */
    public void setIntegralLimit(double maxIntegral) {
        this.maxIntegral = maxIntegral;
    }

    public double calculate(double targetValue, double filteredSensorValue) {
        long newTime = System.nanoTime();
        double deltaTime = (newTime - lastCallTime) / 1e9;
        double error = targetValue - filteredSensorValue;

        //deltaTime = 1;

        // Integral
        integral += 0.5 * (error + lastError) * deltaTime;
        if (integral > maxIntegral) integral = maxIntegral;
        if (integral < -maxIntegral) integral = -maxIntegral;
        //if (lastTargetValue != targetValue) integral = 0;

        // Derivative
        double derivative = (lastTargetValue == targetValue) ? (error - lastError) / deltaTime : 0;

        // No point in increasing the derivative or the integral if this is the first call
        if (lastCallTime == 0) {
            derivative = 0;
            integral = 0;
        }

        // Output
        double output = parameters[0] * error + parameters[1] * integral + parameters[2] * derivative;

        // Save required values for next run
        lastError = error;
        lastCallTime = newTime;
        lastTargetValue = targetValue;

        lastOutput = new double[]{parameters[0] * error, parameters[1] * integral, parameters[2] * derivative};
        return constrain(output);
    }

    public double getIntegral() {
        return this.integral;
    }

    public double[] getLastOutput() {
        return this.lastOutput;
    }
}