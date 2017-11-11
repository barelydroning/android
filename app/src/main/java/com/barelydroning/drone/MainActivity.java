package com.barelydroning.drone;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private OrientationManager orientationManager;

    private final String address = "192.168.1.135";
    private final int port = 5000;
    private int counter = -1;
    private final int DROP_RATE = 20;
    private final ArrayList<Float> azimuthValues = new ArrayList<>();
    private final ArrayList<Float> pitchValues = new ArrayList<>();
    private final ArrayList<Float> rollValues = new ArrayList<>();
    private final int ZERO_SPEED = 1200;

    // Keep a reference to the NetworkFragment, which owns the AsyncTask object
    // that is used to execute network ops.
    private RequestQueue queue;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        queue = Volley.newRequestQueue(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        orientationManager = new OrientationManager(this) {
            @Override
            public void sensorValues(float azimuth, float pitch, float roll) {
                counter = (counter + 1) % DROP_RATE;
                if (counter != 0) {
                    azimuthValues.add(azimuth);
                    pitchValues.add(pitch);
                    rollValues.add(roll);
                    return;
                }

                float avgAzimuth = getAvg(azimuthValues);
                float avgPitch = getAvg(pitchValues);
                float avgRoll = getAvg(rollValues);
                azimuthValues.clear();
                pitchValues.clear();
                rollValues.clear();

                azimuthView.setText(String.format("Azimuth : %.2f", avgAzimuth));
                pitchView.setText(String.format("Pitch: %.2f", avgPitch));
                rollView.setText(String.format("Roll: %.2f", avgRoll));

                String url = String.format("http://%s:%d?&pitch=%f&roll=%f&speed=%d",
                        address, port, avgPitch, avgRoll, ZERO_SPEED);
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
        };

        orientationManager.register();
    }
}
