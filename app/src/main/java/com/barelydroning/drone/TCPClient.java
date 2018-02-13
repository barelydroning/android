package com.barelydroning.drone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.SystemClock;
import android.util.Log;

public class TCPClient {

    private static final String SERVER_IP = "192.168.0.11";
    private static final int PORT = 64000;

    private boolean isRunning = false;

    private TCPMessageListener listener;

    private BufferedReader incomingBuffer;

    private String serverMessage = null;

    public TCPClient(TCPMessageListener listener) {
        this.listener = listener;
    }

    public void stop() {
        isRunning = false;

        listener = null;
        incomingBuffer = null;
        serverMessage = null;
    }

    public void run() {
        isRunning = true;

        try {
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);

            Socket socket = new Socket(serverAddress, PORT);

            try {

                incomingBuffer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while (isRunning) {
                    serverMessage = incomingBuffer.readLine();
                    if (serverMessage != null && listener != null) {
                        listener.onMessage(serverMessage);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                socket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public interface TCPMessageListener {
        void onMessage(String message);
    }



}