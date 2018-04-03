package com.example.thechateau;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;

/**
 * Created by russgomez on 3/27/18.
 */

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.java_websocket.drafts.Draft;
//import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

public class ChatWebSocket extends WebSocketClient {

    //private boolean _openCalled = false;
    private Context _MainContext;
    private MainActivity _MainActivity;


    /*public boolean isOpen()
    {
        return _openCalled;
    }*/


    private String              _WSTAG = "WebSocket";

    public ChatWebSocket(URI serverUri, MainActivity activity) {
        super(serverUri);

        _MainActivity = activity;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        //send("Hello, it is me. Mario :)");

        Log.i(_WSTAG, "new connection opened");
        //_openCalled = true;

        _MainActivity.onConnectedToServer();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i(_WSTAG,"closed with exit code " + code + " additional info: " + reason);

        _MainActivity.onServerDisconnect();
    }

    @Override
    public void onMessage(String message) {
        Log.i(_WSTAG, "received message: " + message);

        _MainActivity.onChatServerMessageReceived(message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        Log.i(_WSTAG, "received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        Log.i(_WSTAG, "an error occurred:" + ex);
    }

    /*public static void main(String[] args) throws URISyntaxException {
        WebSocketClient client = new ChatWebSocket(new URI("ws://localhost:8887"));
        client.connect();
    }*/
}