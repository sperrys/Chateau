package com.example.thechateau;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebSocketClient     _WSClient;
    private String              _WSHOST      = "ws://websockethost:8080";
    private ListView            _ChatListView;
    private final String[]      _ChatListStrings = {"Spencer", "Russ", "Fahad", "Joe"};
    private LinkedList<String>  _ChatListEntries;
    private ArrayAdapter        _ChatListAdapter;
    private Button              _AddNewChatButton;
    private Button              _AddChatToTopButton;
    private int                 _newChatCounter       = 0;
    private static String        _CurrentUser;

    public static String getCurrentUser()
    {
        return _CurrentUser;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _CurrentUser = "User1";

        // Get the Add New Chat Button
        _AddNewChatButton = findViewById(R.id.addChatButton);
        // Set Click Listener for the button
        _AddNewChatButton.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view) {

                        // Add a new chat to our list
                        _newChatCounter++;
                        AddChatEntryToList("New User " + _newChatCounter);
                    }
                }
        );

        // Get the Add Chat To Top Button
        _AddChatToTopButton = findViewById(R.id.MoveChatToTopButton);
        _AddChatToTopButton.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view) {

                       EditText chatToMove     = findViewById(R.id.ChatToAddToTop);
                       String chatToMoveString = chatToMove.getText().toString();

                       // Check if string exists in the list
                       if(_ChatListEntries.contains(chatToMoveString))
                       {
                            // Removes chat from list
                            _ChatListEntries.remove(chatToMoveString);

                            // Adds chat to top of list
                            _ChatListEntries.addFirst(chatToMoveString);

                            // Tells list to update itself
                            _ChatListAdapter.notifyDataSetChanged();

                       };
                    }
                }
        );

        Log.i("Setup", "In Setup");

        // Make array list of strings so we can dynamically add
        _ChatListEntries = new LinkedList<String>(Arrays.asList(_ChatListStrings));

        // Make an adapter for the Chat List view and set it
        _ChatListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, _ChatListEntries);

        // Get the Chat List View from UI
        _ChatListView = (ListView) findViewById(R.id.chat_list_view);
        _ChatListView.setAdapter(_ChatListAdapter);
        _ChatListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {

                    String chatName = (String)_ChatListView.getItemAtPosition(position);

                    _AddNewChatButton.setText(chatName);


                    Log.i("ListView Click Listener", "Got string: " + chatName);
                    Log.i("ListView Click Listener","Clicked element with pos: " + position + " and id: " + id );

                    // Open a new chat window for that specific chat
                    openChatWindow(chatName);
                }

            });


    }

    // Opens a new activity with the chat window
    private void openChatWindow(String chatName)
    {
        // Declare intent of starting activity
        Intent chatHistory = new Intent(this, ChatWindow.class);

        // Tell activity which chat we will be using
        chatHistory.putExtra("chatName", chatName);


        // Start the activity
        startActivity(chatHistory);
    }

    private void AddChatEntryToList(String s) {

            // Add item to list of entries
            _ChatListEntries.add(s);

            // Notify Adapter that data has changed
            _ChatListAdapter.notifyDataSetChanged();
    }


    /*private void connectWebSocket() {

        // Make a URI to connect to the server
        URI uri;
        try {
            uri = new URI(_WSHOST);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        _WSClient = new WebSocketClient(uri) {

            // Called when client first successfully connects with server
            @Override
            public void onOpen(ServerHandshake serverHandshake) {

                Log.i("Websocket", "Opened");
                _WSClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }

            // Called when the client receives a message
            @Override
            public void onMessage(String s) {

                final String message = s;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        //TextView textView = (TextView) findViewById(R.id.messages);

                        //textView.setText(textView.getText() + "\n" + message);
                    }
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };

        // Connect the webclient
        _WSClient.connect();
    }*/


}
