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
import com.example.thechateau.R;

public class MainActivity extends AppCompatActivity
                          implements ChatWindowFragment.OnFragmentInteractionListener{

    private String _defaultPreviewMessage = "No Message History";
    private String _sentPreviewText       = "Sent: ";
    private String _readPreviewText       = "Received: ";

    private class MessageAck
    {
        private int _messageID;
        private long _messageStatus;

        MessageAck(int messageID, long messageStatus)
        {
            _messageID     = messageID;
            _messageStatus = messageStatus;
        }

        public int getID() {
            return _messageID;
        }

        public long getStatus()
        {
            return _messageStatus;
        }
    };

    public int _currentMessageID = 0;

    private class ChatMessagePair {
        private String  _chatname;
        private Message _message;

        ChatMessagePair(String chatname, Message message)
        {
            _chatname = chatname;
            _message  = message;
        }

        public Message getMessage() {
            return _message;
        }

        public String getChatname()
        {
            return _chatname;
        }

    }

    private class Chat {
        List<Message> _chatHistory;
        boolean _isGroupChat;

        Chat(List<Message> chatHistory, boolean isGroupChat)
        {
            _chatHistory = chatHistory;
            _isGroupChat = isGroupChat;
        }

        List<Message> getChatHistory()
        {
            return _chatHistory;
        }

        boolean IsGroupChat()
        {
            return _isGroupChat;
        }
    }

    private boolean             _WSConnected = false;

    private ChatWebSocket       _WSClient;
    private String              _WSHOST      = "ws://10.0.2.2:5000/ws";
    private String              _HerokuHost  = "ws://chateautufts.herokuapp.com:80/ws";
    URI                         _ServerURI;

    private ArrayList<MessageAck>                  _AllMessageConfirmations  = new ArrayList<>();
    private ArrayList<String>                      _MessageSentConfirmations = new ArrayList();
    private ArrayList<String>                      _GroupInitConfirmations   = new ArrayList<>();
    private ArrayList<ChatMessagePair>             _MessagesReceived   = new ArrayList<>();

    private ListView                                _ChatListView;
    private final String[]                          _SampleChatListStrings = {"Spencer", "Russ", "Fahad", "Joe"};
    private LinkedList<ChatListItem>                _ChatListEntries;
    private static Hashtable<String, Chat>          _Chats = new Hashtable<>();


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

    private String openChatWindowString = "Opening ChatWindow: ";

    private Runnable _SetConnectingText = new Runnable() {
        @Override
        public void run() {

            _ConnectingText.setVisibility(View.VISIBLE);
            _ConnectingText.setText("Connecting...");
        }
    };

    private Runnable UpdateChatList = new Runnable() {
        @Override
        public void run() {

            _ChatListAdapter.notifyDataSetChanged();
        }
    };

    public boolean getGroupChatBool(String chatName)
    {
        return _Chats.get(chatName).IsGroupChat();
    }


    private Button              _AddNewChatButton;
    private Button              _AddChatToTopButton;
    private int                 _newChatCounter       = 0;
    private String              _CurrentUser;

    private final String _RegisterRequest    = "RegisterRequest";
    private final String _RegisterResponse   = "RegisterResponse";
    private final long   _RegistrationApprovedCode = 200;
    private final long   _UserAlreadyRegisteredCode = 302;

    private final String _GroupInitExample             = "SingleExample";
    private final String _SingleMessageResponseExample = "GroupExample";

    private final String _GroupMessageInitRequest   = "GroupMessageInitRequest";
    private final String _GroupMessageInitResponse  = "GroupMessageInitResponse";
    private final long   _NewGroupChatCreatedCode       = 201;
    private final long   _GroupChatCreationApprovedCode = 200;

    private final String _GeneralMessageSendRequest  = "MessageRequest";
    private final String _GeneralMessageSendResponse = "MessageSendResponse";
    private final String _GeneralMessageRecv         = "MessageRecv";
    private final long   _GeneralMessageSentSuccessfullyCode = 200;

    private final String _ClientListRequest  = "ClientListRequest";
    private final String _ClientListResponse = "ClientListResponse";
    private final long   _ClientListProvidedCode = 200;

    private final int _GetRandomContactType     = 5;
    private final int _SendSingleMessageType    = 6;


    private final String _RandomMessageResponse     = "RandomMessageResponse";



    private FragmentManager _FragmentManager;

    public String getCurrentUser()
    {
        return _CurrentUser;
    }

    // Returns a chat history with the given name
    public static List<Message> getChatHistory(String chatName)
    {
        return _Chats.get(chatName).getChatHistory();
    }

    public List<ChatListItem> getChatList() {
        return _ChatListEntries;
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
        _ChatListEntries = new LinkedList<>();

        for (String chatName: _SampleChatListStrings)
        {
            _ChatListEntries.add(new ChatListItem(chatName, _defaultPreviewMessage));
        }


        // Make an adapter for the Chat List view and set it
        _ChatListAdapter = new ChatListAdapter(this, R.layout.chat_list_item_row, _ChatListEntries);

        // Set up Chat List View from UI
        // If a chat list item is clicked, it opens a chat window activity
        _ChatListView = findViewById(R.id.chat_list_view);
        _ChatListView.setAdapter(_ChatListAdapter);
        _ChatListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {

                    ChatListItem item = (ChatListItem)_ChatListView.getItemAtPosition(position);
                    String chatName = item.chatName;

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
        for (ChatListItem chatListItem : _ChatListEntries)
        {
            String chatName = chatListItem.chatName;
            List <Message> chatHistory = new ArrayList<Message>();

            Chat newChat = new Chat(chatHistory, false);

            _Chats.put(chatName, newChat);
        }


        /********************/
        /* Set up WebSocket */
        /********************/

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

        /*// Sample Code to check if Spencer chat reads its pending messages when opening
        Message m = new Message("Hi Russ", new User("Spencer"), System.currentTimeMillis());
        ChatMessagePair newPair = new ChatMessagePair("Spencer", m);
        _MessagesReceived.add(newPair);
        _MessagesReceived.add(newPair);
        _MessagesReceived.add(newPair);*/
    }



    /**********************************************************************************************/
    /*                                  Add Chat Functions                                        */
    /**********************************************************************************************/

    private void startRandomChat()
    {
        // Set loading bar on UI screen with messages showing retrieval process

        // Ask server for Random person to chat

        // Check if random person is already being chatted with?
        // -But what if they exist in a 1-1 chat you already have?

        // Open chat window of random chat created
    }

    // Adds a new chat name to the list view and to our list of chats
    // Returns true if a new chat was added for the arguments specified,
    // False otherwise (i.e. chat already exists)
    public boolean AddChat(String chatName, boolean isGroupChat) {

        // Add a chat history for the chat name if necessary
        Chat newChat = _Chats.get(chatName);

        if (newChat == null)
        {
            // Add item to list of entries
            _ChatListEntries.add(new ChatListItem(chatName, _defaultPreviewMessage));

            // Notify Adapter that chatListItems has changed
            runOnUiThread(UpdateChatList);//_ChatListAdapter.notifyDataSetChanged();

            List<Message> chatHistory = new ArrayList<Message>();

            newChat = new Chat(chatHistory, isGroupChat);

            _Chats.put(chatName, newChat);

            moveChatToTop(chatName);

            return true;
        }
        else
        {
            Log.i("AddChat", "ERROR chat already exists");
            return false;
        }

    }

    // Moves a chat to the top of the list view (i.e. to show the most recent chat)
    // If the chat doesn't exist, it does nothing
    public void moveChatToTop(String chatToMoveString)
    {
        Log.i("moveChatToTop", "in moveChatToTop");
        ChatListItem chatToMove = getChatListItemWithChatName(_ChatListEntries, chatToMoveString);
        // Check if string exists in the list

        if(chatToMove != null)
        {
            Log.i("moveChatToTop", "Moving chat with name " + chatToMove.chatName);

            RunMoveChatToTop(chatToMove);
        };
    }

    // Helper function to MoveChatToTop
    // Uses runnable to run the move chat operation on the UI Thread
    private void RunMoveChatToTop(final ChatListItem chatToMove) {
        Runnable moveChat = new Runnable() {
            @Override
            public void run() {
                // Removes chat from list
                _ChatListEntries.remove(chatToMove);

                // Adds chat to top of list
                _ChatListEntries.addFirst(chatToMove);

                // Tells list to update itself
                _ChatListAdapter.notifyDataSetChanged();
            }
        };

        runOnUiThread(moveChat);
    }


    // Searches through a list of chat names and checks if one of them has a chatName
    // that matches the chatname argument
    // Returns the ChatListItem if found, null otherwise
    public ChatListItem getChatListItemWithChatName(List<ChatListItem> chatListItems, String chatName)
    {
        for(ChatListItem item: chatListItems)
        {
            if (item.chatName.equals(chatName))
            {
                return item;
            }
        }
        return null;
    }

    /**********************************************************************************************/
    /*                                  Fragment Functions                                        */
    /**********************************************************************************************/

    // Opens a new fragment with the chat window for the given chat name
    private void openChatWindow(String chatName)
    {
        _FragmentManager = this.getSupportFragmentManager();

        Log.i("openChatWindow", "starting chat window fragment");

        FragmentTransaction fragmentTransaction = _FragmentManager.beginTransaction();

        ChatWindowFragment chatWindowFragment = ChatWindowFragment.newInstance(chatName);
        fragmentTransaction.add(R.id.fragment_container, chatWindowFragment);
        fragmentTransaction.addToBackStack(openChatWindowString + chatName);

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

    // Have to override this so returning fragments don't leave main activity blank
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Make the activity's components visible again
        findViewById(R.id.nonFragmentStuff).setVisibility(View.VISIBLE);
        //findViewById(R.id.fragment_container).setVisibility(View.INVISIBLE);
    }


    /**********************************************************************************************/
    /*                                  Chat Message Functions                                    */
    /**********************************************************************************************/

    // Sends a chat message to the server
    // Returns true if the messsage was sent successfully
    // Returns false if the message couldn't be sent
    public boolean sendChatMessageToServer(String chatName, String content, boolean isGroupChat)
    {
        JSONObject json = new JSONObject();

        try
        {
            json.put("recipient", chatName);
            json.put("content"  , content);
            json.put("type", _GeneralMessageSendRequest);

            /*if(isGroupChat)
            {
                json.put("type"     , _SendGroupMessage);
            }
            else
            {
                json.put("type"     , _SendSingleMessage);
            }*/

        } catch (JSONException e)
        {
            e.printStackTrace();
            return false;
        }

        String message = json.toString();


        Log.i("SendChatMessageToServer", "Sending chat message(): " + message);
        sendMessageToServer(message);

        if(waitUntilMessageSent(3000))
        {

            return true;
        }
        else
        {
            return false;
        }
    }

    // Waits for timetoWaitMS milliseconds for a message confirmation to be received
    // If it receives a message confirmation in that time, it returns true
    // Else, returns false
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
                _MessageSentConfirmations.remove(_SingleMessageResponseExample);

                return true;
            }
        }


        return true;
    }

    // Returns the status code of a message if it was found in the list of message confirmations
    // Returns -1 if the message was never received
    private long waitUntilMessageAcked(int messageID, long timeToWaitMS)
    {
        String tag = "waitUntilMessageAcked";
        // Calc time that we will timeout
        long timeOutExpiredTimeMS = System.currentTimeMillis() + timeToWaitMS;


        while (1 == 1)
        {
            Log.i(tag, "Waiting for message");

            long waitMs = timeOutExpiredTimeMS - System.currentTimeMillis();

            if (waitMs <= 0)
            {
                Log.i(tag, "reached timeout after waiting for ACK");
                return -1;
            }

            // Check if we got a message acknowledgement for the messageID
            if (_AllMessageConfirmations.size() > 0)
            {
                Log.i(tag, "MESSAGE ACK stack > 0");

                Iterator<MessageAck> iter = _AllMessageConfirmations.iterator();

                while (iter.hasNext())
                {
                    MessageAck msgAck = iter.next();

                    int currentID = msgAck.getID();

                    Log.i(tag, "currentID is " + currentID + " with status " + msgAck.getStatus());

                    // If you we find the right message ack, remove it and return it's status
                    if (currentID == messageID)
                    {
                        iter.remove();
                        return msgAck.getStatus();
                    }

                }
            }
        }
    }


    /**********************************************************************************************/
    /*                           Server Connection Functions                                      */
    /**********************************************************************************************/

    // Prompt WS client to connect to the server
    private void callWSConnect()
    {
        Log.i("MainActivity", "Calling WSConnect()");
        _WSClient.connect();
    }

    // Called by WebSocketClient when it has connected to the server
    public void onConnectedToServer() {
        // Update text view that says connecting or not

        Log.i("OnConnectedToServer", "In function");

        // Indicate to everyone that we're connected to the server
        _WSConnected = true;

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

    // Called by the websocket client when the server disconnects
    public void onServerDisconnect()
    {
        Log.i("MainActivity", "Called OnServerDisconnect");

        _WSConnected = false;

        // Remove fragments from the display
        removeAllFragments();

        // User is no longer registered when chats disconnect
        _RegisteredUser = false;

        // Try reconnecting to the server
        _WSClient = new ChatWebSocket(_ServerURI, this);
        callWSConnect();

        // Set connected attribute to "connecting"
        runOnUiThread(_SetConnectingText);

    }


    /**********************************************************************************************/
    /*                           User Registration Functions                                      */
    /**********************************************************************************************/

    // Register current client in chat server
    // Returns true if the user gets registered in a given amount of time, false otherwise
    public boolean registerUser(String username, String password, boolean doAuthentication)
    {

        JSONObject json = new JSONObject();

        try {
            json.put("type",     _RegisterRequest);
            json.put("username", username);
            json.put("password", password);

            // TODO change to be variable authentication
            json.put("auth", doAuthentication);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        String message = json.toString();


        Log.i("MainActivity", "Sending registration message(): " + message);
        sendMessageToServer(message);

        // Wait to see if client gets a registration response in 2 seconds, if not return false;
        if(waitUntilRegistered(2000))
        {
            _CurrentUser = username;
            return true;
        }

        else
        {
            return false;
        }

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

    /**********************************************************************************************/
    /*                           Miscellaneous Functions                                          */
    /**********************************************************************************************/

    // Called when a message is received from the chat server (called in ChatWebSocket)
    public void onChatServerMessageReceived(String message)
    {
        org.json.simple.JSONObject  jsonObject;

        JSONParser parser  = new JSONParser();

        String tag = "OnChatServerMsgReceived";

        Log.i(tag, "New Message Received: " + message);

        try
        {
            jsonObject = (org.json.simple.JSONObject)parser.parse(message);

            String type = (String)jsonObject.get("type");
            long status = (long)jsonObject.get("status");

            Log.i(tag, "Received type: " + type);

            switch (type)
            {
                case _RegisterResponse:
                {
                    // If user was successfully registered
                    if (status == _RegistrationApprovedCode )
                    {
                        Log.i(tag, "Registration successful");

                        _RegisteredUser = true;

                    }

                    // If user is already registered
                    else if (status == _UserAlreadyRegisteredCode)
                    {
                        Log.i(tag, "Error, username was already registered");
                        // Tell user to pick a new username
                    }

                    else
                    {
                        Log.i(tag, "Error, status is " + status);
                    }

                    int messageID = 1;
                    _AllMessageConfirmations.add(new MessageAck(messageID, status));

                    break;
                }

                case _GroupMessageInitResponse:
                {
                    String chatName = (String) jsonObject.get("chatname");
                    Log.i(tag, "Received groupMessageInitResponse");

                    // Indicates that a group chat this user created was approved by the server
                    if(status == _GroupChatCreationApprovedCode)
                    {
                        Log.i(tag, "Success, status is " + status);
                        _GroupInitConfirmations.add(_GroupInitExample);
                    }

                    // Indicates that a group was created by another user that includes this user
                    else if (status == _NewGroupChatCreatedCode)
                    {
                        // Add a new chat for this chatName if it doesn't exist yet
                        if (_Chats.get(chatName) == null)
                        {
                            AddChat(chatName, true);
                        }

                    }
                    else
                    {
                        Log.i(tag, "Error, status is " + status);
                    }

                    int messageID = 2;

                    // Add confirmation if status doesn't indicate that group chat was newly created
                    if (status != _NewGroupChatCreatedCode)
                    {
                        _AllMessageConfirmations.add(new MessageAck(messageID, status));
                    }

                    break;
                }

                case _ClientListResponse:
                {
                    Log.i(tag, "Got " + _ClientListResponse);

                    // Update our contact list using the server's list of clients
                    if(status == _ClientListProvidedCode)
                    {
                        org.json.simple.JSONArray jsonArray = (org.json.simple.JSONArray)jsonObject.get("clients");
                        String[] contacts = new String[jsonArray.size()];

                        for(int i = 0; i < jsonArray.size(); i++)
                        {
                            String contact = (String)jsonArray.get(i);

                            //Log.i(tag, "current contact:" + contact);

                            if(!contact.equals(_CurrentUser) && !contact.equals(""))
                            {
                                //Log.i(tag, "adding contact:" + contact);
                                contacts[i] = (String) jsonArray.get(i);
                            }
                        }

                        Log.i(tag, "Converting to array");
                        _ContactList = new ArrayList(Arrays.asList(contacts));
                    }

                    int messageID = 3;
                    _AllMessageConfirmations.add(new MessageAck(messageID, status));

                    break;
                }

                case _RandomMessageResponse:
                {
                    break;
                }

                case _GeneralMessageSendResponse:
                {
                    Log.i(tag, "Got message send response");

                    if(status == _GeneralMessageSentSuccessfullyCode)
                    {
                        Log.i(tag, "Adding to message confirmations");
                        _MessageSentConfirmations.add(_SingleMessageResponseExample);

                    }

                    int messageID = 4;
                    _AllMessageConfirmations.add(new MessageAck(messageID, status));

                    break;

                }

                case _GeneralMessageRecv:
                {
                    Log.i("ChatServerMsgRecvGen", "Got Message Received");

                    if (status == 200 || status == 201)
                    {
                        String content      = (String) jsonObject.get("content");
                        String sender       = (String) jsonObject.get("sender");
                        String chatName     = (String) jsonObject.get("chatname");
                        boolean isGroupChat = (boolean) jsonObject.get("groupchat");

                        // Add a new chat for this chatName if it doesn't exist yet
                        if(_Chats.get(chatName) == null)
                        {
                            AddChat(chatName, isGroupChat);
                        }

                        // Add the new message to our list of received messages
                        Message newMessage = new Message(content, new User(sender), System.currentTimeMillis());
                        ChatMessagePair newPair = new ChatMessagePair(chatName, newMessage);
                        _MessagesReceived.add(newPair);

                        // Update the preview and notification icon of the chat
                        updateChatMessagePreviewAndNotification(chatName, content, false);

                        // Check if the chat window is open for that chat
                        // If open, tell the chat to update its message history
                        // If not tell the chat there's a new message waiting
                        ChatWindowFragment chatWindow = getChatWindowFragment(chatName);

                        if(chatWindow != null)
                        {
                            Log.i(tag, "Telling chat to update itself");
                            chatWindow.onReceivedMessage();
                        }
                        else
                        {
                            setChatNotified(chatName, true);
                        }
                    }
                    break;
                }


                default:
                {
                    Log.i(tag, "Error, unknown type " + type);
                }
            }
        }
        catch (Exception e)
        {
            Log.i("Parsing", "Error " + e.getMessage());
        }

    }

    // Check if the chat window with the name chatName is currently open
    // Returns the chatwindow fragment with the specified name
    // Returns null if the fragment isn't found
    ChatWindowFragment getChatWindowFragment(String chatName)
    {
        Log.i("MainActivity", "Called getChatWindowFragment");

        for(Fragment fragment: getSupportFragmentManager().getFragments())
        {

            if (fragment!=null)
            {
                if(fragment.getClass().equals(ChatWindowFragment.class))
                {
                    if(((ChatWindowFragment)fragment).getChatName().equals(chatName))
                    {
                        Log.i("getChatWindowFragment", "found fragment with name " + chatName);
                        return (ChatWindowFragment)fragment;
                    }
                }

            }

        }

        return null;
    }

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
        sendMessageToServer(message);

        // Wait a few seconds for contact list to be retrieved, then return contact list
        waitUntilTimeReached(2000);

        Log.i("requestContactList", "List of current contacts");

        for(String s: _ContactList)
        {
            Log.i("requestContactList", "contact:" + s);
        }

        // Remove current user from contact list (user will never need to chat with themselves
        //_ContactList = removeNameFromContactList(_ContactList, _CurrentUser);

        return _ContactList;

    }

    private ArrayList<String> removeNameFromContactList(ArrayList<String> contacts, String currentUser)
    {
        String tag = "RemoveNameFromContacts";

        Log.i(tag, "in " + tag + " for " + currentUser);

        for (int i = 0; i < contacts.size(); i++)
        {
            String contact = contacts.get(i);

            Log.i(tag, "current contact " + contact);

            if (contact.equals(currentUser))
            {
                Log.i(tag, "Removing " + currentUser);
                contacts.remove(i); // Remove the string, and then the spot in the array list?

                break;
            }
        }
        return contacts;
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

    // Checks for new messages for this chat using main's MessagesReceivedList and
    // removes them from main and adds them here if it finds any
    public void checkForNewMessages(String chatName)
    {
        Log.i("ChatWindowFragment", "in checkForNewMessages()");

        // Get the chat's chatHistory
        List<Message> chatHistory = _Chats.get(chatName).getChatHistory();

        if (chatHistory == null)
        {
            Log.i("ChatWindowFragment", "Error chat history for " + chatName + " is null");
        }

        Iterator<ChatMessagePair> iter = _MessagesReceived.iterator();

        // Check if we've any received messages for this chat
        while (iter.hasNext())
        {
            ChatMessagePair m = iter.next();

            String messageChatName = m.getChatname();

            Log.i("checkForNewMessages()", "requested chatname is " + chatName+ "and current chatname is " + messageChatName);

            // If we found a message for this chat
            // -Add it to the chat history for that chat
            // -Remove from main's received message list
            if (messageChatName.equals(chatName))
            {
                chatHistory.add(m.getMessage());
                iter.remove();
            }
        }

    }
    public boolean sendChatRegistrationToServer(List<String> contactsToAdd, String chatName)
    {
        // Single chats don't need to be registered to the server
        if (contactsToAdd.size() == 1)
        {
            return true;
        }

        // Otherwise, send a group chat
        JSONObject json = new JSONObject();


        try {

            JSONArray jsonArrayBetter =  new JSONArray();

            for (String contact: contactsToAdd)
            {
                jsonArrayBetter.put(contact);
            }

            json.put("type"      , _GroupMessageInitRequest);
            json.put("recipients", (Object)jsonArrayBetter);
            json.put("chatname"  , chatName);

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String message = json.toString();

        Log.i("SendChatRegisToServer", "Sending group init message(): " + message);
        sendMessageToServer(message);

        if(waitUntilGroupMessageInitResponseReceived(3000))
        {
            Log.i("SendChatRegisToServer", "Received init response");
            return true;
        }
        else
        {
            Log.i("SendChatRegisToServer", "Did not receive init response");
            return false;
        }
    }


    // Waits until the web socket client is connected, and then sends a message
    private void sendMessageToServer(String message)
    {
        Log.i("sendMessageToServer", "in sendMessageToServer()");
        while (_WSClient == null || _WSConnected != true);

        _WSClient.send(message);
    }

    // Waits for timetoWaitMS milliseconds for a GroupInitResponse to be received
    // If we receives a confirmation in that time, return true
    // Else, returns false
    private boolean waitUntilGroupMessageInitResponseReceived(long timeToWaitMS)
    {
        // Calc time that we will timeout
        long timeOutExpiredTimeMS = System.currentTimeMillis() + timeToWaitMS;

        boolean initConfirmationReceived = false;

        while (!initConfirmationReceived)
        {
            Log.i("waitUntilMessageSent", "Waiting for message");

            long waitMs = timeOutExpiredTimeMS - System.currentTimeMillis();

            if (waitMs <= 0)
            {
                Log.i("waitUntilMessageSent", "reached timeout for waiting for message");
                return false;
            }

            if (_GroupInitConfirmations.size() >= 1)
            {
                Log.i("waitUntilMessageSent", "found message confirmation");
                _GroupInitConfirmations.remove(_GroupInitExample);

                return true;
            }
        }

        return true;
    }

    public void updateChatMessagePreviewAndNotification(String chatName, String content, boolean isSent)
    {
        Log.i("updatePreview()", "in updateChatMessage Preview");

        // Update the chat message preview if it exists
        //ChatListItem chat = getChatListItemWithChatName(_ChatListEntries, chatName);

        String newPreviewMessage = "";

        /*if (isSent )
            newPreviewMessage = _sentPreviewText;
        else
            newPreviewMessage = _readPreviewText;
*/
        newPreviewMessage = content;

        ((ChatListAdapter)_ChatListAdapter).setPreviewMessage(chatName, newPreviewMessage);



        // Create a runnable action to run on the UI thread
        Runnable updateMsgPreview = new Runnable() {
            @Override
            public void run() {
                _ChatListAdapter.notifyDataSetChanged();
            }
        };

        Log.i("updatePreview()", "running adapter on UI thread");

        // Run action to update chatListAdapter
        runOnUiThread(updateMsgPreview);



    }

    // Sets a chats notified attribute, indicating whether it should notify a new message or not
    public void setChatNotified(final String chatName, final boolean isNotified)
    {
        Log.i("setChatNotified", "in setChatNotified for chat " + chatName);

        Runnable _makeChatNotified = new Runnable() {
            @Override
            public void run() {
                ((ChatListAdapter)_ChatListAdapter).setNotified(chatName, isNotified);
            }
        };

        runOnUiThread(_makeChatNotified);
    }
}
