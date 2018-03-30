package com.example.thechateau;


import android.app.Activity;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.json.simple.parser.JSONParser;

public class MainActivity extends AppCompatActivity
                          implements ChatWindowFragment.OnFragmentInteractionListener{

    private ChatWebSocket       _WSClient;
    private String              _WSHOST      = "ws://10.0.2.2:5000/ws";
    private String              _HerokuHost  = "ws://chateautufts.herokuapp.com:80/ws";

    private ListView                                _ChatListView;
    private final String[]                          _SampleChatListStrings = {"Spencer", "Russ", "Fahad", "Joe"};
    private LinkedList<String>                      _ChatListEntries;
    private static Hashtable<String, List<Message>> _ChatHistories = new Hashtable<>();

    private ArrayList<String>   _ContactList = new ArrayList<>();

    private ArrayAdapter        _ChatListAdapter;

    private boolean             _enteredUsername = false;




    private Button              _AddNewChatButton;
    private Button              _AddChatToTopButton;
    private int                 _newChatCounter       = 0;
    private String              _CurrentUser;

    private final int _RegisterType             = 1;
    //private final String _RegisterType = "2";
    private final int _SendMessageToClientsType = 3;
    private final int _GetClientsListType       = 4;
    private final int _GetRandomContactType     = 5;
    private final int _SendSingleMessageType    = 6;


    private FragmentManager _FragmentManager;

    public String getCurrentUser()
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
                    public void onClick(View view)
                    {

                        // Open an Add Chat Fragment
                        openAddChatWindow();
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
            uri = new URI(_HerokuHost);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        // Connect to web server
        Log.i("MainActivity", "Calling SetupWS()");
        _WSClient = new ChatWebSocket(uri, this);

        Log.i("MainActivity", "Calling WSConnect()");
        _WSClient.connect();

        // Wait to connect
        while(_WSClient == null || !_WSClient.isOpen())
        {

            Log.i("MainActivity", "Websocket client is still null");
            //this.finish();
            //System.exit(-4);
        }


        registerCurrentUser();

    }

    // Register current client in chat server
    private void registerCurrentUser()
    {

        JSONObject json = new JSONObject();

        try {
            json.put("type",     _RegisterType);
            json.put("username", _CurrentUser);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        String message = json.toString();


        Log.i("MainActivity", "Sending registration message(): " + message);
        _WSClient.send(message);
    }

    // Request a contact list until it gets populated, then returns populated contact list
    private ArrayList<String> getContactList()
    {
        while (_ContactList == null || _ContactList.size() > 1)
        {
            requestContactList();
        }

        return _ContactList;
    }

    // Request a contact list from the chat server
    private void requestContactList()
    {
        JSONObject json = new JSONObject();

        try
        {
            json.put("type",     _GetClientsListType);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        String message = json.toString();

        Log.i("MainActivity", "Sending getClientMessage(): " + message);
        _WSClient.send(message);

    }

    private void startGetUsernameFragment()
    {


    }

    private void startRandomChat()
    {
        // Set loading bar on UI screen with messages showing retrieval process

        // Ask server for Random person to chat

        // Check if random person is already being chatted with?
        // -But what if they exist in a 1-1 chat you already have?

        // Open chat window of random chat created
    }

    // Adds a new chat name to the list view
    public void AddChat(String chatName) {

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

    private void openAddChatWindow()
    {
        _FragmentManager = this.getSupportFragmentManager();

        Log.i("openAddChatWindow", "starting add chat fragment");

        FragmentTransaction fragmentTransaction = _FragmentManager.beginTransaction();

        AddChatFragment addChatFragment = new AddChatFragment();

        fragmentTransaction.add(R.id.fragment_container, addChatFragment);
        fragmentTransaction.addToBackStack("Adding AddChatFragment");

        fragmentTransaction.commit();
    }

    // Have to override this so returning fragments don't leave main activity blank
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Make the activity's components visible again
        findViewById(R.id.nonFragmentStuff).setVisibility(View.VISIBLE);
    }

    // Called when a message is received from the chat server (called in ChatWebSocket)
    public void onChatServerMessageReceived(String message)
    {
        org.json.simple.JSONObject  jsonObject;
        JSONParser parser = new JSONParser();
        int type;

        Log.i("OnChatServerMsgReceived", "New Message Received: " + message);

        try
        {
            jsonObject = (org.json.simple.JSONObject)parser.parse(message);
            // jsonObject.keySet();

            for (Object key: jsonObject.keySet())
            {
                Log.i("OnChatSeverMsgReceived", "Key: " + key.toString() + ", Value: " + jsonObject.get(key).toString());
            }

        }
        catch (Exception e)
        {
            Log.i("Parsing", "Error " + e.getMessage());
        }
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
