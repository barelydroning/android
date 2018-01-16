package com.barelydroning.drone;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

/**
 * Created by andreas on 2017-11-19.
 */

public class UDPClient {

    private final String TAG = UDPClient.class.getSimpleName();

    private UDPListener listener;
    private DatagramSocket socket;
    private InetAddress ipAddress;
    private int port;

    public UDPClient(UDPListener listener) {
        this.listener = listener;
    }

    public void connect(String ipAddress, int port) {
        try {
            this.ipAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        //socket.connect(this.ipAddress, port);

        this.port = port;

//        byte[] sendData = new byte[1024];
//        byte[] receiveData = new byte[1024];
//        String sentence = inFromUser.readLine();
//        sendData = sentence.getBytes();
//        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
//        clientSocket.send(sendPacket);
//        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//        clientSocket.receive(receivePacket);
//        String modifiedSentence = new String(receivePacket.getData());
//        System.out.println("FROM SERVER:" + modifiedSentence);
//        clientSocket.close();
    }

    public void send(String message) {
        new Thread(new ClientSend(message)).start();
//        byte[] data = message.getBytes(Charset.forName("UTF-8"));
//        DatagramPacket sendPacket = new DatagramPacket(data, data.length, ipAddress, port);
//        try {
//            socket.send(sendPacket);
//            Log.i(TAG, "packages sent with message: " + message);
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e(TAG, "sending error");
//        }
    }

    public class ClientSend implements Runnable {

        private String message;

        public ClientSend(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            try {
                byte[] buf = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, ipAddress, port);
                socket.setBroadcast(true);
                socket.send(packet);
                Log.i(TAG, "packages sent with message: " + message);
            } catch (SocketException e) {
                Log.e("Udp:", "Socket Error:", e);
            } catch (IOException e) {
                Log.e("Udp Send:", "IO Error:", e);
            }
            socket.disconnect();
        }
    }

    public void disconnect() {
        socket.close();
    }

    private class UDPThread extends Thread {

        public UDPThread() {

        }

        @Override
        public void run() {
            super.run();
        }
    }



    public interface UDPListener {
        void onMessage();
    }

}
