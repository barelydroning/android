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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.gson.Gson;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements UDPClient.UDPListener {

    private OrientationManager orientationManager;

    private static final String TAG = "MainActivity";

    private final String address = "192.168.0.11";
    private final int port = 5000;
    private final ArrayList<Float> azimuthValues = new ArrayList<>();
    private final ArrayList<Float> pitchValues = new ArrayList<>();
    private final ArrayList<Float> rollValues = new ArrayList<>();

    private final String SERVER_IP = "192.168.0.13";
    private final int PORT = 8080;


    private final static int TARGET_HZ = 300;

    private long lastSensorValueTime;
    private int tmpTimeCount;

    private boolean serialPortConnected = false;

    private UsbManager usbManager;

    private RequestQueue queue;

//    private UDPClient udpClient;

    private UDPCommunicator udpCommunicator;

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

    private boolean DEBUG_WITHOUT_SERIAL = false;

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
        String prettyString = String.format("\n\nMotor A: %d\nMotor B: %d\nMotor C: %d\nMotor D: %d\nMotor E: %d\nMotor F: %d\n\n", motorSpeedA, motorSpeedB, motorSpeedC, motorSpeedD, motorSpeedE, motorSpeedF);
        System.out.println(prettyString);
        if (!DEBUG_WITHOUT_SERIAL) serialPort.write(serialString.getBytes());
        //Log.i(TAG, prettyString);
    }

    private int motorSpeedA = 1100;
    private int motorSpeedB = 1100;
    private int motorSpeedC = 1100;
    private int motorSpeedD = 1100;
    private int motorSpeedE = 1100;
    private int motorSpeedF = 1100;

    private int BASE_SPEED = DEBUG_WITHOUT_SERIAL ? 0 : 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        queue = Volley.newRequestQueue(this);

        URI uri = null;
//        try {
//            uri = new URI("ws://" + SERVER_IP + ":" + PORT);
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//        udpCommunicator = new UDPCommunicator(uri);
//        udpCommunicator.connect();

        //new TCPClient(this).start(SERVER_IP);

//        udpClient = new UDPClient(this);
//        udpClient.connect(SERVER_IP, PORT);


        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();

        if (!DEBUG_WITHOUT_SERIAL) {
            UsbDevice device = devices.get(devices.keySet().toArray()[0]);
            usbManager.requestPermission(device, mPermissionIntent);
        }

        final Gson gson = new Gson();

        int kp = 600;
        double ki = 600;
        double kd = 0;

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

                if (azimuthValues.isEmpty() || tmpTimeCount == 0 || (1000 / tmpTimeCount) > TARGET_HZ) {
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

                Data data = new Data(avgPitch, avgRoll, avgAzimuth, 0);

//                udpCommunicator.sendMessage(gson.toJson(data));
//                udpClient.send(gson.toJson(data));

                //Log.i(TAG, "Roll pid value is " + rollPidValue);
                if (DEBUG_WITHOUT_SERIAL || serialPortConnected) {

                    motorSpeedA = BASE_SPEED - pitchPidValue;
                    motorSpeedC = BASE_SPEED - pitchPidValue;

                    motorSpeedD = BASE_SPEED + pitchPidValue;
                    motorSpeedF = BASE_SPEED + pitchPidValue;


                    motorSpeedB = 1000;
                    motorSpeedE = 1000;

                    writeToArduino(motorSpeedA, motorSpeedB, motorSpeedC, motorSpeedD, motorSpeedE, motorSpeedF);

                    double[] lastOutput = pitchPid.getLastOutput();

                    String url = String.format("http://%s:%d?pitch=%f&motorSpeedA=%d&motorSpeedC=%d&motorSpeedD=%d&motorSpeedF=%d&pitchIntegral=%f&pitchP=%f&pitchI=%f&pitchD=%f",
                            address, port, avgPitch, motorSpeedA, motorSpeedC, motorSpeedD, motorSpeedF, pitchPid.getIntegral(), lastOutput[0], lastOutput[1], lastOutput[2]).replace(",", ".");
                    // Request a string response from the provided URL.
                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d(getClass().getName(), "Success");
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(getClass().getName(), "Error");
                        }
                    });
                    queue.add(stringRequest);

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

//    @Override
//    public void onConnectionEstablished() {
//        Log.i(TAG, "onConnectionEstablished");
//    }
//
//    @Override
//    public void onConnectionLost() {
//        Log.i(TAG, "onConnectionLost");
//    }
//
//    @Override
//    public void onMessageReceived(String message) {
//        Log.i(TAG, "onMessageReceived with message: " + message);
//    }

    @Override
    public void onMessage() {
        Log.i(TAG, "onMessage with message TODO");
    }
}
