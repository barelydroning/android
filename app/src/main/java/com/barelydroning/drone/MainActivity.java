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
import android.os.AsyncTask;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

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

    private String droneId = null;

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

    @BindView(R.id.main_text)
    TextView mainText;

    // Pitch PID views
    @BindView(R.id.pitch_p)
    TextView pitchP;

    @BindView(R.id.pitch_i)
    TextView pitchI;

    @BindView(R.id.pitch_d)
    TextView pitchD;

    // Roll PID views
    @BindView(R.id.roll_p)
    TextView rollP;

    @BindView(R.id.roll_i)
    TextView rollI;

    @BindView(R.id.roll_d)
    TextView rollD;

    // Motors views
    @BindView(R.id.motor_a)
    TextView motorA;

    @BindView(R.id.motor_b)
    TextView motorB;

    @BindView(R.id.motor_c)
    TextView motorC;

    @BindView(R.id.motor_d)
    TextView motorD;

    @BindView(R.id.motor_e)
    TextView motorE;

    @BindView(R.id.motor_f)
    TextView motorF;


    private float getAvg(Collection<Float> values) {
        float sum = 0;
        for (Float f : values) {
            sum += f;
        }
        return sum / values.size();
    }

    private UsbDeviceConnection connection;

    private UsbSerialDevice serialPort;

    private boolean DEBUG_WITHOUT_SERIAL = true;

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

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String command;
                    try {
                        command = data.getString("command");
                        mainText.setText(command);

                        if (command.equals("kill")) {
                            motorSpeedA = 1000;
                            motorSpeedB = 1000;
                            motorSpeedC = 1000;
                            motorSpeedD = 1000;
                            motorSpeedE = 1000;
                            motorSpeedF = 1000;
                        }

                        Log.i(TAG, command);
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        return;
                    }

                }
            });
        }
    };

    private int motorSpeedA = 1100;
    private int motorSpeedB = 1100;
    private int motorSpeedC = 1100;
    private int motorSpeedD = 1100;
    private int motorSpeedE = 1100;
    private int motorSpeedF = 1100;

    private Socket socket;

    private int BASE_SPEED = DEBUG_WITHOUT_SERIAL ? 0 : 1000;

    private static final int DROP_RATE = 5;

    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        try {
            socket = IO.socket("http://192.168.0.13:3001");

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {


                @Override
                public void call(Object... args) {
                    Log.i(TAG, "SOCKET CONNECTED");
                    socket.emit("connect_drone");
                    //socket.disconnect();
                }

            })
            .on("command", onNewMessage)
            .on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.i(TAG, "SOCKET DISCONNECTED");
                }

            })
            .on("socket_id", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    droneId = (String) args[0];
                    Log.i(TAG, "DRONE ID IS: " + droneId);
                }
            })
            ;
            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.i(TAG, "COMMAND RECEIVED");

                JSONObject obj = (JSONObject)args[0];


                System.out.println("");
                System.out.println(obj);
                System.out.println("");
                //System.out.println("COMMAND RECEIVED: " + obj.)
            }

        }

        ;


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

                counter = (counter + 1) % DROP_RATE;


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



//                    writeToArduino(motorSpeedA, motorSpeedB, motorSpeedC, motorSpeedD, motorSpeedE, motorSpeedF);

                    double[] lastOutputPitch = pitchPid.getLastOutput();

                    pitchP.setText(String.format("P: %f", lastOutputPitch[0]));
                    pitchI.setText(String.format("I: %f", lastOutputPitch[1]));
                    pitchD.setText(String.format("D: %f", lastOutputPitch[2]));

                    double[] lastOutputRoll = rollPid.getLastOutput();

                    rollP.setText(String.format("P: %f", lastOutputRoll[0]));
                    rollI.setText(String.format("I: %f", lastOutputRoll[1]));
                    rollD.setText(String.format("D: %f", lastOutputRoll[2]));

                    motorA.setText(String.format("A: %d", motorSpeedA));
                    motorB.setText(String.format("B: %d", motorSpeedB));
                    motorC.setText(String.format("C: %d", motorSpeedC));
                    motorD.setText(String.format("D: %d", motorSpeedD));
                    motorE.setText(String.format("E: %d", motorSpeedE));
                    motorF.setText(String.format("F: %d", motorSpeedF));


                    if (droneId != null && counter == 0) {
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("drone", droneId);
                            obj.put("pitch", avgPitch);
                            obj.put("roll", avgRoll);
                            obj.put("azimuth", avgAzimuth);
                            obj.put("motorSpeedA", motorSpeedA);
                            obj.put("motorSpeedB", motorSpeedB);
                            obj.put("motorSpeedC", motorSpeedC);
                            obj.put("motorSpeedD", motorSpeedD);
                            obj.put("motorSpeedE", motorSpeedE);
                            obj.put("motorSpeedF", motorSpeedF);
                            obj.put("pitchIntegral", pitchPid.getIntegral());
                            obj.put("pitchP", lastOutputPitch[0]);
                            obj.put("pitchI", lastOutputPitch[1]);
                            obj.put("pitchD", lastOutputPitch[2]);

                            socket.emit("drone_data", obj);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }


//                    String url = String.format("http://%s:%d?pitch=%f&motorSpeedA=%d&motorSpeedC=%d&motorSpeedD=%d&motorSpeedF=%d&pitchIntegral=%f&pitchP=%f&pitchI=%f&pitchD=%f",
//                            address, port, avgPitch, motorSpeedA, motorSpeedC, motorSpeedD, motorSpeedF, pitchPid.getIntegral(), lastOutputPitch[0], lastOutputPitch[1], lastOutputPitch[2]).replace(",", ".");
//                    // Request a string response from the provided URL.
//                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
//                            new Response.Listener<String>() {
//                                @Override
//                                public void onResponse(String response) {
//                                    Log.d(getClass().getName(), "Success");
//                                }
//                            }, new Response.ErrorListener() {
//                        @Override
//                        public void onErrorResponse(VolleyError error) {
//                            Log.d(getClass().getName(), "Error");
//                        }
//                    });
//                    queue.add(stringRequest);

                }
            }
        };

        orientationManager.register();
    }

    private TCPClient tcpClient;

    public class ConnectTask extends AsyncTask<String, Integer, TCPClient> {

        @Override
        protected TCPClient doInBackground(String... message) {
//we create a TCPClient object and
            tcpClient = new TCPClient(new TCPClient.TCPMessageListener() {
                @Override
//here the messageReceived method is implemented
                public void onMessage(String message) {
                    Log.i("Debug","Input message: " + message);
                }
            });
            tcpClient.run();

            return null;
        }
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
