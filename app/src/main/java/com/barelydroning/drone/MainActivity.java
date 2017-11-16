package com.barelydroning.drone;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private OrientationManager orientationManager;

    private static final String TAG = "MainActivity";

    private final String address = "192.168.0.11";
    private final int port = 5000;
    private final ArrayList<Float> azimuthValues = new ArrayList<>();
    private final ArrayList<Float> pitchValues = new ArrayList<>();
    private final ArrayList<Float> rollValues = new ArrayList<>();


    private final static int TARGET_HZ = 20;

    private long lastSensorValueTime;
    private int tmpTimeCount;

    private boolean serialPortConnected = false;

    private UsbManager usbManager;

    @BindView(R.id.main_azimuth)
    TextView azimuthView;

    @BindView(R.id.main_pitch)
    TextView pitchView;

    @BindView(R.id.main_roll)
    TextView rollView;

    private float getAvg(Collection<Float> values) {
        float sum = 0;
        for (Float f : values) {
            sum += f;
        }
        return sum / values.size();
    }

    private UsbDeviceConnection connection;

    private UsbSerialDevice serialPort;

    // A callback for received data must be defined
    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback()
    {
        @Override
        public void onReceivedData(byte[] arg0)
        {
            // Code here
        }
    };

    private void writeToArduino(int motorSpeedA, int motorSpeedB, int motorSpeedC, int motorSpeedD, int motorSpeedE, int motorSpeedF) {
        String serialString = String.format("%dA%dB%dC%dD%dE%dFT", motorSpeedA, motorSpeedB, motorSpeedC, motorSpeedD, motorSpeedE, motorSpeedF);
        String prettyString = String.format("\nMotor A: %d\nMotor B: %d\nMotor C: %d\nMotor D: %d\nMotor E: %d\nMotor F: %d", motorSpeedA, motorSpeedB, motorSpeedC, motorSpeedD, motorSpeedE, motorSpeedF);
        serialPort.write(serialString.getBytes());
        Log.i(TAG, prettyString);
    }

    private int motorSpeedA = 1000;
    private int motorSpeedB = 1000;
    private int motorSpeedC = 1000;
    private int motorSpeedD = 1000;
    private int motorSpeedE = 1000;
    private int motorSpeedF = 1000;

    private int BASE_SPEED = 1100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        System.currentTimeMillis();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();

        UsbDevice device = devices.get(devices.keySet().toArray()[0]);

        usbManager.requestPermission(device, mPermissionIntent);

        int kp = 250;
        int ki = 20;
        int kd = 10;

        final PID rollPid = new PID(kp, ki, kd);
        final PID pitchPid = new PID(kp, ki, kd);

        lastSensorValueTime = System.currentTimeMillis();

        orientationManager = new OrientationManager(this) {
            @Override
            public void sensorValues(float azimuth, float pitch, float roll) {
                long now = System.currentTimeMillis();
                long dt = now - lastSensorValueTime;
                lastSensorValueTime = now;

                tmpTimeCount += dt;

                if (tmpTimeCount != 0 && (1000 / tmpTimeCount) > TARGET_HZ) {
                    azimuthValues.add(azimuth);
                    pitchValues.add(pitch);
                    rollValues.add(roll);
                    return;
                }
                tmpTimeCount = 0;

                float avgAzimuth = getAvg(azimuthValues);
                float avgPitch = getAvg(pitchValues);
                float avgRoll = getAvg(rollValues);
                azimuthValues.clear();
                pitchValues.clear();
                rollValues.clear();

                azimuthView.setText(String.format("Azimuth : %.2f", avgAzimuth));
                pitchView.setText(String.format("Pitch: %.2f", avgPitch));
                rollView.setText(String.format("Roll: %.2f", avgRoll));

                int rollPidValue = (int) rollPid.calculate(0, avgRoll);
                int pitchPidValue = (int) pitchPid.calculate(0, avgPitch);

                Log.i(TAG, "Roll pid value is " + rollPidValue);
                if (serialPortConnected) {

                    motorSpeedA = BASE_SPEED + rollPidValue;
                    motorSpeedB = BASE_SPEED + rollPidValue;

                    motorSpeedD = BASE_SPEED - rollPidValue;
                    motorSpeedE = BASE_SPEED - rollPidValue;

                    motorSpeedC = BASE_SPEED;
                    motorSpeedF = BASE_SPEED;

                    writeToArduino(motorSpeedA, motorSpeedB, motorSpeedC, motorSpeedD, motorSpeedE, motorSpeedF);
                }
            }
        };

        orientationManager.register();
    }




    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            connection = usbManager.openDevice(device);
                            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                            if(serialPort != null)
                            {
                                if(serialPort.open())
                                {
                                    serialPort.setBaudRate(115200);
                                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                                    serialPort.read(mCallback);

                                    serialPortConnected = true;
                                }
                            }
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

}
