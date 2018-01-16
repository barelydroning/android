package com.barelydroning.drone;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.URI;

/**
 * Created by andreas on 2017-11-19.
 */

public class UDPCommunicator {

    private final String TAG = UDPCommunicator.class.getSimpleName();

    private WebSocketClient socket;

    private boolean isConnected = false;

    public UDPCommunicator(URI uri) {


        this.socket = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i(TAG, "websocket open");
                isConnected = true;
            }

            @Override
            public void onMessage(String message) {
                Log.i(TAG, "websocket message: " + message);
            }


            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i(TAG, "websocket closed");
                isConnected = false;
            }

            @Override
            public void onError(Exception ex) {
                isConnected = false;
                Log.i(TAG, "websocket error");
            }
        };

    }

    public void connect() {
        this.socket.connect();
    }

    public void sendMessage(String message) {
        if (isConnected) {
            socket.send(message);
        }
    }


}
