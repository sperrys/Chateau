package com.example.thechateau;


import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
    URI                         _ServerURI;

    private ArrayList<Integer>                      _MessageSentConfirmations = new ArrayList();
    private ArrayList<Message>                      _SingleMessagesReceived   = new ArrayList<>();

    private ListView                                _ChatListView;
    private final String[]                          _SampleChatListStrings = {"Spencer", "Russ", "Fahad", "Joe"};
    private LinkedList<String>                      _ChatListEntries;
    private static Hashtable<String, List<Message>> _ChatHistories = new Hashtable<>();

    private ArrayList<String>   _ContactList = new ArrayList<>();

    private ArrayAdapter        _ChatListAdapter;

    private boolean             _RegisteredUser = false; // True if user has been registered

    private TextView       _ConnectingText;
    private RelativeLayout _ConnectingLayout;

    private Runnable _SetConnectedText = new Runnable() {
        @Override
        public void run() {

            _ConnectingText.setVisibility(View.VISIBLE);
            _ConnectingText.setText("Connected!");
        }
    };

    private Runnable _SetConnectingText = new Runnable() {
        @Override
        public void run() {

            _ConnectingText.setVisibility(View.VISIBLE);
            _ConnectingText.setText("Connecting...");
        }
    };


    public ArrayList<Message> getSingleMessagesReceived()
    {
        return _SingleMessagesReceived;
    }

    private Button              _AddNewChatButton;
    private Button              _AddChatToTopButton;
    private int                 _newChatCounter       = 0;
    private String              _CurrentUser;

    private final String _RegisterRequest    = "RegisterRequest";
    private final String _RegisterResponse   = "RegisterResponse";

    private final String _SendSingleMessage         = "SingleMessageRequest";
    private final String _SingleMessageResponse     = "SingleMessageResponse";
    private final String _SingleMessageRecvResponse = "SingleMessageRecvResponse";

    private final String _GroupMessageInitRequest   = "GroupMessageInitRequest";
    private final String _GroupMessageInitResponse  = "GroupMessageInitResponse";

    private final String _SendGroupMessage          = "GroupMessageRequest";
    private final String _GroupMessageResponse      = "GroupMessageResponse";
    private final String _GroupMessageRecv          = "GroupMessageRecv";


    private final String _ClientListRequest = "ClientListRequest";

    private final int _GetRandomContactType     = 5;
    private final int _SendSingleMessageType    = 6;

    // Response Strings

    private final String _ClientListResponse        = "ClientListResponse";
    private final String _RandomMessageResponse     = "RandomMessageResponse";



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



        _ConnectingText   = findViewById(R.id.ConnectingText);
        _ConnectingLayout = findViewById(R.id.ConnectingLayout);

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

        // Set up URI to connect to the server

        try {
            _ServerURI = new URI(_HerokuHost);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        // Connect to web server
        Log.i("MainActivity", "Calling SetupWS()");
        _WSClient = new ChatWebSocket(_ServerURI, this);

        callWSConnect();

        Message m = new Message("Hi Russ", new User("Spencer"), System.currentTimeMillis());

        _SingleMessagesReceived.add(m);
        _SingleMessagesReceived.add(m);
        _SingleMessagesReceived.add(m);
    }

    // Prompt WS client to connect to the server
    private void callWSConnect()
    {
        Log.i("MainActivity", "Calling WSConnect()");
        _WSClient.connect();
    }


    // Register current client in chat server
    // Returns true if the user gets registered in a given amount of time, false otherwise
    public boolean registerUser(String username, String password)
    {

        JSONObject json = new JSONObject();

        try {
            json.put("type",     _RegisterRequest);
            json.put("username", username);
            json.put("password", password);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        String message = json.toString();


        Log.i("MainActivity", "Sending registration message(): " + message);
        _WSClient.send(message);

        // Wait to see if client gets a registration response in 2 seconds, if not return false;
        if(waitUntilRegistered(2000))
        {
            _CurrentUser = username;
            return true;
        }

        else return false;

    }

    /*// Request a contact list until it gets populated, then returns populated contact list
    private ArrayList<String> getContactList()
    {
        while (_ContactList == null || _ContactList.size() > 1)
        {
            requestContactList();
        }

        return _ContactList;
    }*/

    // Request a contact list from the chat server
    public ArrayList<String> requestContactList()
    {
        JSONObject json = new JSONObject();

        try
        {
            json.put("type", _ClientListRequest);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        String message = json.toString();

        Log.i("MainActivity", "Sending getClientMessage(): " + message);
        _WSClient.send(message);

        // Wait a few seconds for contact list to be retrieved, then return contact list
        waitUntilTimeReached(2000);

        Log.i("requestContactList", "List of current contacts");

        for(String s: _ContactList)
        {
            Log.i("requestContactList", "contact:" + s);
        }

        return _ContactList;

    }

    // Runs until the timeToWaitMS has been reached, then returns
    private void waitUntilTimeReached(long timeToWaitMS)
    {
        // Calc time that we will timeout
        long timeOutExpiredTimeMS = System.currentTimeMillis() + timeToWaitMS;

        boolean messageConfirmationReceived = false;

        while (1 == 1) {
            Log.i("waitUntilTimeReached", "Waiting");

            long waitMs = timeOutExpiredTimeMS - System.currentTimeMillis();

            if (waitMs <= 0) {

                Log.i("waitUntilTimeReached", "reached timeout for waiting for message");
                return;
            }


        }
    }

    private void startLoginFragment()
    {
        _FragmentManager = this.getSupportFragmentManager();

        Log.i("openLoginWindow", "starting Login fragment");

        FragmentTransaction fragmentTransaction = _FragmentManager.beginTransaction();

        LoginFragment loginFragment = new LoginFragment();
        fragmentTransaction.add(R.id.fragment_container, loginFragment);

        fragmentTransaction.addToBackStack("Adding Login Window");

        fragmentTransaction.commit();

    }

    // Checks for new messages for this chat using main's singleMessagesReceivedList and
    // removes them from main and adds them here if it finds any
    public void checkForNewMessages(String chatName)
    {
        Log.i("ChatWindowFragment", "in checkForNewMessages()");

        // Get the chat's chatHistory
        List<Message> chatHistory = _ChatHistories.get(chatName);

        Iterator<Message> iter = _SingleMessagesReceived.iterator();


        // Check if any received messages
        while (iter.hasNext())
        {
            Message m = iter.next();
            String senderName = m.getSender().getName();
            Log.i("checkForNewMessages()", "chatname is " + chatName+ "and sender name is " + senderName);

            if (senderName.equals(chatName))
            {
                // Add it to our message list and remove it from main's received message list
                chatHistory.add(m);

                iter.remove();
            }
        }
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
        //int type;

        Log.i("OnChatServerMsgReceived", "New Message Received: " + message);

        try
        {
            jsonObject = (org.json.simple.JSONObject)parser.parse(message);

            String type = (String)jsonObject.get("type");
            long status = (long)jsonObject.get("status");
            Log.i("OnChatSeverMsgReceived", "Received type: " + type);

            switch (type)
            {
                case _RegisterResponse:
                {
                    // If user was successfully registered
                    if (status == 200 ) {
                        Log.i("OnChatServerMsgReceived", "Registration successful");

                        _RegisteredUser = true;

                        //_CurrentUser    = (String)jsonObject.get("username");
                        //Log.i("OnChatServerMsgReceived", "current user is now " + _CurrentUser);
                    }

                    // If user is already registered
                    else if (status == 302)
                    {
                        Log.i("OnChatServerMsgReceived", "Error, username was already registered");
                        // Tell user to pick a new username
                    }

                    else {
                        Log.i("OnChatServerMsgReceived", "Error, status is " + status);
                    }
                    break;
                }

                case _GroupMessageInitResponse:
                {

                    break;
                }

                case _GroupMessageResponse:
                {

                    break;
                }

                case _GroupMessageRecv:
                {

                    break;
                }

                case _ClientListResponse:
                {
                    Log.i("OnChatServerMsgReceived", "Got ClientListResponse");

                    if(status == 200)
                    {
                        org.json.simple.JSONArray jsonArray = (org.json.simple.JSONArray)jsonObject.get("clients");

                        _ContactList = new ArrayList<>();

                        String[] contacts = new String[jsonArray.size()];

                        for(int i = 0; i < jsonArray.size(); i++)
                        {
                            if(!contacts[i].equals(_CurrentUser))
                            {
                                contacts[i] = (String) jsonArray.get(i);
                            }
                        }

                        _ContactList = new ArrayList(Arrays.asList(contacts));
                    }

                    break;
                }

                case _RandomMessageResponse:
                {
                    break;
                }

                case _SingleMessageResponse:
                {
                    Log.i("OnChatServerMsgReceived", "Got Single Message response");
                    if(status == 200)
                    {
                        Log.i("OnChatServerMsgReceived", "Adding to message confirmations");
                        _MessageSentConfirmations.add(1);
                    }
                    break;
                }
                case _SingleMessageRecvResponse:
                {
                    Log.i("OnChatServerMsgReceived", "Got Single Message Received");

                    if (status == 200)
                    {
                        String content = (String) jsonObject.get("content");
                        String sender  = (String) jsonObject.get("sender");


                        Message newMessage = new Message(content, new User(sender), System.currentTimeMillis());


                    }
                    break;
                }



                default:
                {
                    Log.i("OnChatServerMsgReceived", "Error, unknown type " + type);
                }
            }
        }
        catch (Exception e)
        {
            Log.i("Parsing", "Error " + e.getMessage());
        }
    }

    public boolean sendChatMessageToServer(String chatName, String content, boolean isGroupChat)
    {
        JSONObject json = new JSONObject();

        try {

            json.put("recipient", chatName);
            json.put("content"  , content);

            if(isGroupChat)
            {
                json.put("type"     , _SendGroupMessage);
            }
            else
            {
                json.put("type"     , _SendSingleMessage);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String message = json.toString();


        Log.i("SendChatMessageToServer", "Sending chat message(): " + message);
        _WSClient.send(message);

        if(waitUntilMessageSent(3000))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean waitUntilMessageSent(long timeToWaitMS)
    {
        // Calc time that we will timeout
        long timeOutExpiredTimeMS = System.currentTimeMillis() + timeToWaitMS;

        boolean messageConfirmationReceived = false;

        while (!messageConfirmationReceived)
        {
            Log.i("waitUntilMessageSent", "Waiting for message");

            long waitMs = timeOutExpiredTimeMS - System.currentTimeMillis();

            if (waitMs <= 0)
            {
                Log.i("waitUntilMessageSent", "reached timeout for waiting for message");
                return false;
            }

            if (_MessageSentConfirmations.size() >= 1)
            {
                Log.i("waitUntilMessageSent", "found message confirmation");
                return true;
            }
            // Check if the message is in our list of message sent confirmations
            /*for(Integer m: _MessageSentConfirmations)
            {
                if(m.getMessage().equals(content));
                {
                    Log.i("waitUntilMessageSent", "message confirmation with content \"" + content + "\"");
                    _MessageSentConfirmations.remove(m);
                    return true;
                }

            }*/

        }


        return true;
    }

    // Called by WebSocketClient when it has connected to the server
    public void onConnectedToServer() {
        // Update text view that says connecting or not

        Log.i("OnConnectedToServer", "In function");

        // Register current user with the server if necessary
        if(!_RegisteredUser) startLoginFragment();

        // Set the text in the connected layout to "connected!"
        runOnUiThread(_SetConnectedText);

        // Wait for a few seconds and then make the connecting layout view disappear
        Thread waiter = new Thread() {
            @Override
            public void run()
            {

                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e)
                {
                    Log.i("WaiterThread", "Exception! " + e.getMessage());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.i("WaiterThread", "Setting ConnectingLayout to Invisible");
                        _ConnectingLayout.setVisibility(View.INVISIBLE);
                    }
                });
            }
        };
        waiter.run();

    }




    public void onServerDisconnect()
    {
        Log.i("MainActivity", "Called OnServerDisconnect");

        removeAllFragments();

        _RegisteredUser = false;

        _WSClient = new ChatWebSocket(_ServerURI, this);

        callWSConnect();

        // Set connected attribute to "connecting"
        runOnUiThread(_SetConnectingText);

    }

    // Loops for a given amount of time until the user is registered
    // Returns true if user was registered in a given amount of time, false otherwise
    private boolean waitUntilRegistered(long timeToWaitMS)
    {
        // Calc time that we will timeout
        long timeOutExpiredTimeMS = System.currentTimeMillis() + timeToWaitMS;

        while (!_RegisteredUser)
        {
            Log.i("waitUntilRegistered", "Waiting for register timeout");

            long waitMs = timeOutExpiredTimeMS - System.currentTimeMillis();

            if (waitMs <= 0)
            {
                Log.i("waitUntilRegistered", "Timed out");
                return false;
            }

        }

        Log.i("waitUntilRegistered", "user became registered");
        return true;
    }

    private void removeAllFragments()
    {
        Log.i("MainActivity", "Called RemoveAllFragments");
        for(Fragment fragment:getSupportFragmentManager().getFragments())
        {
            /*if(fragment instanceof NavigationDrawerFragment)
            {
                continue;
            }
            else
            {*/
                if(fragment!=null)
                {
                    getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                }
            //}
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


}
