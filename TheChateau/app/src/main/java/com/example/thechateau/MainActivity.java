package com.example.thechateau;


import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity
                          implements ChatWindowFragment.OnFragmentInteractionListener{

    private ChatWebSocket       _WSClient;
    private String              _WSHOST      = "ws://localhost:5000/ws";

    private ListView                                _ChatListView;
    private final String[]                          _SampleChatListStrings = {"Spencer", "Russ", "Fahad", "Joe"};
    private LinkedList<String>                      _ChatListEntries;
    private static Hashtable<String, List<Message>> _ChatHistories = new Hashtable<>();

    private ArrayAdapter        _ChatListAdapter;


    private Button              _AddNewChatButton;
    private Button              _AddChatToTopButton;
    private int                 _newChatCounter       = 0;
    private static String       _CurrentUser;

    private final String _RegisterType = "1";


    private FragmentManager _FragmentManager;

    public static String getCurrentUser()
    {
        return _CurrentUser;
    }

    // Returns a chat history with the given name
    public static List<Message> getChatHistory(String chatName)
    {
        return _ChatHistories.get(chatName);
    }

    @Override
    public void onFragmentInteraction(Uri uri){
        //you can leave it empty
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set current user
        _CurrentUser = "User1";

        /******************************/
        /* Set up Add New Chat Button */
        /******************************/
        _AddNewChatButton = findViewById(R.id.addChatButton);
        _AddNewChatButton.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view) {

                        EditText _chatToAdd = findViewById(R.id.ChatToAdd);
                        String newChatName  = _chatToAdd.getText().toString();

                        // Add a new chat to our list
                        //_newChatCounter++;
                        AddChat(newChatName);

                        // Clear the text box
                        _chatToAdd.getText().clear();
                    }
                }
        );

        Log.i("Setup", "In Setup");

        /*************************/
        /* Set up Chat List View */
        /*************************/

        // Make linked list of strings so we can easily add elements to front of list
        _ChatListEntries = new LinkedList<String>(Arrays.asList(_SampleChatListStrings));

        // Make an adapter for the Chat List view and set it
        _ChatListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, _ChatListEntries);

        // Set up Chat List View from UI
        // If a chat list item is clicked, it opens a chat window activity
        _ChatListView = (ListView) findViewById(R.id.chat_list_view);
        _ChatListView.setAdapter(_ChatListAdapter);
        _ChatListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {

                    String chatName = (String)_ChatListView.getItemAtPosition(position);

                    //_AddNewChatButton.setText(chatName);

                    Log.i("ListView Click Listener", "Got string: " + chatName);
                    Log.i("ListView Click Listener","Clicked element with pos: " + position + " and id: " + id );

                    // Open a new chat window for that specific chat
                    openChatWindow(chatName);
                }
            });

        /*****************************************************************/
        /* Initialize Sample Chat Histories                              */
        /* (As if these chats were already present when opening the app) */
        /*****************************************************************/
        for (String chatName : _ChatListEntries)
        {
            List <Message> chatHistory = new ArrayList<Message>();

            _ChatHistories.put(chatName, chatHistory);
        }


        /*******************/
        /**Set up WebSocket*/
        /*******************/

        // Make a URI to connect to the server
        URI uri;
        try {
            uri = new URI(_WSHOST);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        // Connect to web server
        Log.i("MainActivity", "Calling SetupWS()");
        _WSClient = new ChatWebSocket(uri);

        Log.i("MainActivity", "Calling WSConnect()");
        _WSClient.connect();


        /*JSONObject json = new JSONObject();

        try {
            json.put("type", _RegisterType);
            json.put("username", _CurrentUser);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        String message = json.toString();



        while(_WSClient == null || !_WSClient.isOpen())
        {
            Log.i("MainActivity", "WS client is still null");
        }

        Log.i("MainActivity", "Sending WS message()");
        _WSClient.send(message);*/


    }

    // Adds a new chat name to the list view
    private void AddChat(String chatName) {

        // Add item to list of entries
        _ChatListEntries.add(chatName);

        // Notify Adapter that data has changed
        _ChatListAdapter.notifyDataSetChanged();

        // Add a chat history for the chat name if necessary
        List <Message> chatHistory = _ChatHistories.get(chatName);

        if (chatHistory == null)
        {
            chatHistory = new ArrayList<Message>();
            _ChatHistories.put(chatName, chatHistory);
        }
        else
        {
            Log.i("AddChat", "ERROR chatHistory wasn't null, but sender didn't exist previously");
        }

    }

    // Moves a chat to the top of the list view (i.e. to show the most recent chat)
    // If the chat doesn't exist, it does nothing
    public void moveChatToTop(String chatToMoveString)
    {
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

    // Opens a new fragment with the chat window for the given chat name
    private void openChatWindow(String chatName)
    {
        _FragmentManager = this.getSupportFragmentManager();

        Log.i("openChatWindow", "starting chat window fragment");

        FragmentTransaction fragmentTransaction = _FragmentManager.beginTransaction();

        ChatWindowFragment chatWindowFragment = ChatWindowFragment.newInstance(chatName);
        fragmentTransaction.add(R.id.fragment_container, chatWindowFragment);
        fragmentTransaction.addToBackStack("Adding chatwindow");

        fragmentTransaction.commit();

    }

    /*private void openAddChatWindow()
    {
        _FragmentManager = this.getSupportFragmentManager();

        Log.i("openChatWindow", "starting add chat fragment");

        FragmentTransaction fragmentTransaction = _FragmentManager.beginTransaction();

        //ChatWindowFragment chatWindowFragment = ChatWindowFragment.newInstance(chatName);
        //AddChatFragment =
        //fragmentTransaction.add(R.id.fragment_container, chatWindowFragment);
        fragmentTransaction.addToBackStack("Adding chatwindow");

        fragmentTransaction.commit();
    }*/

    // Have to override this so returning fragments don't leave main activity blank
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Make the activity's components visible again
        findViewById(R.id.nonFragmentStuff).setVisibility(View.VISIBLE);
    }

    // Called when a message is received by the user
    private void onMessageReceived(Message message, String chatName)
    {

        // Initialize a chat for the sender if necessary
        if(!_ChatListEntries.contains(chatName))
        {
            AddChat(chatName);
        }

        List<Message> chatHistory = getChatHistory(chatName);

        // Initialize a chat history for the person if necessary
        if (chatHistory != null)
        {
            Log.i("OnMessage", "Adding message to chatHistory");
            chatHistory.add(message);

            // Move the chat to the top of the list of chats
            moveChatToTop(chatName);
        }
        else
        {
            Log.i("OnMessage", "ERROR chatHistory was null");
        }
    }




    
    /*private void setupWebSocket()
    {
        // Make a URI to connect to the server
        URI uri;
        try {
            uri = new URI(_WSHOST);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        _WSClient = new WebSocketClient(uri, a) {


            // Called when client first successfully connects with server
            @Override
            public void onOpen(ServerHandshake serverHandshake) {

                Log.i(_WSTAG, "Opened");
                //_WSClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);

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
                Log.i(_WSTAG, "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i(_WSTAG, "Error " + e.getMessage());
            }
        };
    }*/



}
