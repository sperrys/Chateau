package com.example.thechateau;


import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

    /**********************************************************************************************/
    /*                                    Web Socket Stuff                                        */
    /**********************************************************************************************/
    private boolean             _WSConnected = false;
    private ChatWebSocket       _WSClient;
    //private String              _WSHOST      = "ws://10.0.2.2:5000/ws";
    private String              _MainHost  = "ws://chateautufts.herokuapp.com:80/ws";
    URI                         _ServerURI;

    /**********************************************************************************************/
    /*                            Message Housekeeping Structures                                 */
    /**********************************************************************************************/

    private ArrayList<MessageAck>                  _AllMessageConfirmations  = new ArrayList<>();
    private ArrayList<ReceivedMessage>             _MessagesReceived         = new ArrayList<>();

    private class ReceivedMessage {
        private String  _chatname;
        private Message _message;

        ReceivedMessage(String chatname, Message message)
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
    private class MessageAck {
        private long    _messageID;
        private long   _messageStatus;
        private String _messageResponse;

        MessageAck(long messageID, long messageStatus)
        {
            _messageID     = messageID;
            _messageStatus = messageStatus;
            _messageResponse = "";
        }

        MessageAck(long messageID, long messageStatus, String messageResponse)
        {
            _messageID       = messageID;
            _messageStatus   = messageStatus;
            _messageResponse = messageResponse;
        }

        public long getID() {
            return _messageID;
        }

        public long getStatus()
        {
            return _messageStatus;
        }

        public String getMessageResponse()
        {
            return _messageResponse;
        }
    };
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

    /**********************************************************************************************/
    /*                             Chat Lists and Histories                                       */
    /**********************************************************************************************/

    private final String[]                          _SampleChatListStrings = {"Spencer", "Russ", "Fahad", "Joe"};
    private LinkedList<ChatListItem>                _ChatListEntries;
    private ListView                                _ChatListView;
    private ArrayAdapter                            _ChatListAdapter;
    private static Hashtable<String, Chat>          _Chats = new Hashtable<>();

    private Runnable UpdateChatList = new Runnable() {
        @Override
        public void run() {

            _ChatListAdapter.notifyDataSetChanged();
        }
    };


    private ArrayList<String>   _ContactList = new ArrayList<>();

    /**********************************************************************************************/
    /*                             User Registration Variables                                    */
    /**********************************************************************************************/

    private boolean             _UserIsRegistered           = false;
    private String              _CurrentUser;
    private String              _CurrentPassword;
    private boolean             _RegisterWithAuthentication = false;
    private boolean             _UserHasRegisteredBefore    = false;

    private final String  _DefaultUserToken = "";
    private       String  _UserToken        = _DefaultUserToken;


    /**********************************************************************************************/
    /*                             Server / Host Variables                                        */
    /**********************************************************************************************/

    // List of servers we can try to connect to
    private List<String> _ServerHosts            = new ArrayList<>(Arrays.asList(_MainHost));

    private int          _CurrentServerHostIndex = 0; // Index of current host we're trying to connect to
    private int          _HostRetries            = 0; // Number of times we've tried to connect to the current host
    private final int    _MaxHostRetries         = 5;

    /**********************************************************************************************/
    /*                                  Fragment Variables                                        */
    /**********************************************************************************************/

    private FragmentManager _FragmentManager;
    private String           openChatWindowString = "Opening ChatWindow: ";

    /**********************************************************************************************/
    /*                            Connecting Layout Variables                                     */
    /**********************************************************************************************/

    private TextView       _ConnectingText;
    private RelativeLayout _ConnectingLayout;

    /**********************************************************************************************/
    /*                                       Buttons                                              */
    /**********************************************************************************************/

    private Button         _AddNewChatButton;
    private Button         _RandomChatButton;

    /**********************************************************************************************/
    /*                         Message Type And Status Code Definitions                           */
    /**********************************************************************************************/

    /* Message Type And Status Code Definitions */
    public final static String _RegisterRequest               = "RegisterRequest";
    public final static String _RegisterResponse              = "RegisterResponse";
    public final static long   _RegistrationApprovedCode      = 200;
    public final static long   _UserAlreadyRegisteredCode     = 303;
    public final static long   _GenericRegistrationErrorCode  = 400;
    public final static long   _NoServerResponseCode          = -1;

    private final String _GroupMessageInitRequest       = "GroupMessageInitRequest";
    private final String _GroupMessageInitResponse      = "GroupMessageInitResponse";
    private final long   _NewGroupChatCreatedCode       = 201;
    private final long   _GroupChatCreationApprovedCode = 200;

    private final String _GeneralMessageSendRequest          = "MessageRequest";
    private final String _GeneralMessageSendResponse         = "MessageSendResponse";
    private final String _GeneralMessageRecv                 = "MessageRecv";
    private final long   _GeneralMessageSentSuccessfullyCode = 200;

    private final String _ClientListRequest      = "ClientListRequest";
    private final String _ClientListResponse     = "ClientListResponse";
    private final long   _ClientListProvidedCode = 200;



    private final String _RandomMessageRequest  = "RandomMessageRequest";
    private final String _RandomMessageResponse = "RandomMessageResponse";
    private final long   _RandomMessageSuccess  = 200;
    private final long   _RandomMessageError    = 400;
    private final long   _NoOtherUsersErrorCode = 404;

    private final String _ErrorResponse              = "ErrorResponse";
    private final long   _TokenNotAuthenticErrorCode = 301;

    /**********************************************************************************************/
    /*                               Message Preview Variables                                    */
    /**********************************************************************************************/

    private String _defaultPreviewMessage = "No Message History";
    private String _sentPreviewText       = "Sent: ";
    private String _readPreviewText       = "Received: ";

    /**********************************************************************************************/
    /*                                Message ID Variables                                        */
    /**********************************************************************************************/


    private       long   _currentMessageID = 0;
    private final long   _MaxMessageID     = 10000;
    private final String _messageIDField   = "msg_id";

    /**********************************************************************************************/
    /*                                Random Chat Variables                                        */
    /**********************************************************************************************/

    private RelativeLayout _RandomChatInfoLayout;
    private TextView       _RandomChatInfoText;

    /**********************************************************************************************/
    /*                                  Data Access Functions                                     */
    /**********************************************************************************************/

    // Returns true if the chatname specified is a group chat, false otherwise
    public boolean getGroupChatBool(String chatName)
    {
        return _Chats.get(chatName).IsGroupChat();
    }

    // Returns the user currently using the app
    public String getCurrentUser()
    {
        return _CurrentUser;
    }

    // Returns a chat history with the given name
    public static List<Message> getChatHistory(String chatName)
    {
        return _Chats.get(chatName).getChatHistory();
    }

    // Gets the current list of chats displayed on the main screen
    public List<ChatListItem> getChatList() {
        return _ChatListEntries;
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


    @Override
    public void onFragmentInteraction(Uri uri){
        //you can leave it empty
    }


    /**********************************************************************************************/
    /*                      "On Create" Function (Called on Applicatoin Startup)                    */
    /**********************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set current user
        _CurrentUser = "User1";

        // Set up UI elements for the connecting layout
        _ConnectingText   = findViewById(R.id.ConnectingText);
        _ConnectingLayout = findViewById(R.id.ConnectingLayout);

        // Set up UI elements for Random Chats
        _RandomChatInfoLayout = findViewById(R.id.RandomChatInfoLayout);
        _RandomChatInfoText   = findViewById(R.id.RandomChatInfo);

        _RandomChatButton = findViewById(R.id.RandomChatButton);
        _RandomChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRandomChat("Default Chat Message");
            }
        });

        // Set up UI element and click event for Add Chat Button
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

        /**********************************/
        /* Set up List of Chats on the UI */
        /**********************************/

        // Make linked list of strings so we can easily add elements to front of list
        _ChatListEntries = new LinkedList<>();

        for (String chatName: _SampleChatListStrings)
        {
            _ChatListEntries.add(new ChatListItem(chatName, _defaultPreviewMessage));
        }

        // Make an adapter for the Chat List view and set it
        _ChatListAdapter = new ChatListAdapter(this, R.layout.chat_list_item_row, _ChatListEntries);

        // Set up the chat list view so that it opens the appropriate chat window
        // when clicked
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

        // Add default messages to one of our sample chats
        String simepleChatName = "Spencer";

        for (int i = 0; i < 10; i++)
        {
            // Sample Code to check if Spencer chat reads its pending messages when opening
            Message m = new Message("Hi Russ", new User(simepleChatName), System.currentTimeMillis());
            ReceivedMessage newPair = new ReceivedMessage(simepleChatName, m);
            _MessagesReceived.add(newPair);
            _MessagesReceived.add(newPair);

            Message n = new Message("I'm Russ", new User("mgomez"), System.currentTimeMillis());
            _MessagesReceived.add(new ReceivedMessage(simepleChatName, n));
        }

        /********************/
        /* Set up WebSocket */
        /********************/

        // Set up our web socket connection and connect to default host at
        // index 0
        setupWebSocketConnection(_ServerHosts.get(0));
        callWSConnect();

    }

    /**********************************************************************************************/
    /*                                  Add Chat Functions                                        */
    /**********************************************************************************************/

    // Starts a random chat with a username specified by the server
    private void startRandomChat(String content)
    {
        // TODO Set loading bar on UI screen with messages showing retrieval process?

        JSONObject json = new JSONObject();

        try
        {
            //json.put("content"  , content);
            json.put("type", _RandomMessageRequest);

        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        String message = json.toString();

        // Ask server for Random person to chat
        MessageAck messageAck = sendMessageToServer(json, _RandomMessageRequest);

        // If we a get a positive response from the server, open
        // up a chat with the username it specified
        // Otherwise, display an error message saying the chat could not be created
        if (messageAck != null)
        {
            Long status = messageAck.getStatus();

            if (status == _RandomMessageSuccess)
            {
                org.json.simple.JSONObject jsonObject;

                JSONParser parser = new JSONParser();

                try
                {
                    jsonObject = (org.json.simple.JSONObject) parser.parse(messageAck.getMessageResponse());

                    String clientName = (String) jsonObject.get("client");

                    Log.i("startRandomChat", "clientName is " + clientName);

                    // Add new chat if necessary
                    if (_Chats.get(clientName) == null)
                    {
                        AddChat(clientName, false);
                    }

                    openChatWindow(clientName);

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else if (status == _NoOtherUsersErrorCode)
            {
                brieflyDisplayRandomChatInfo("Error: No other users online");
            }

        }
        else
        {
            brieflyDisplayRandomChatInfo("Error: No server response");
            Log.i("RandomChat",  "Error, Random chat could not be created");

        }

        // TODO Check if random person is already being chatted with?
    }

    // Displays info for a Random Chat for 2 seconds
    // in the Random Chat text layout, and then disappears from the view
    private void brieflyDisplayRandomChatInfo(String infoMessage)
    {
        _RandomChatInfoLayout.setVisibility(View.VISIBLE);
        _RandomChatInfoText.setVisibility(View.VISIBLE);
        _RandomChatInfoText.setText(infoMessage);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                _RandomChatInfoLayout.setVisibility(View.INVISIBLE);
                _RandomChatInfoText.setVisibility(View.INVISIBLE);
            }
        }, 2000);
    }

    // Removes the connecting text from the screen after a certain delay has been reached
    private void RemoveConnectingTextAfterDelay(final long delayMillis)
    {
        Log.i("RemoveConnectingText", "Removing Connecting Text after " + delayMillis + " millis");
        Thread waiter = new Thread() {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(delayMillis);
                }
                catch (InterruptedException e)
                {
                    Log.i("WaiterThread", "Exception! " + e.getMessage());
                }

                setConnectingLayout(View.GONE, "");
                //runOnUiThread(_RemoveConnectingText);
            }
        };
        waiter.run();
    }

    // Set the visibility and message of the connecting layout that appears
    // on the screen when a disconnect from the server occurs
    private void setConnectingLayout(final int visibility, final String connectMessage)
    {
        Log.i("setConnectingLayout", "Setting connected layout to " + visibility + " with message " + connectMessage);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _ConnectingLayout.setVisibility(visibility);
                _ConnectingText.setText(connectMessage);
            }
        });
    }

    // Adds a new chat name to the list view and to our list of chats
    // Returns true if a new chat was added for the arguments specified,
    // False otherwise (i.e. chat already exists)
    public boolean AddChat(String chatName, boolean isGroupChat)
    {

        // Add a chat history for the chat name if necessary
        Chat newChat = _Chats.get(chatName);

        if (newChat == null)
        {
            // Add item to list of entries
            _ChatListEntries.add(new ChatListItem(chatName, _defaultPreviewMessage));

            // Notify Adapter that chatListItems has changed
            runOnUiThread(UpdateChatList);

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

    // Starts the AddChatFragment, which brings the user to the screen for adding chats
    // to their list of chats
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


    // Starts the login fragment, which launches the loginfragment UI and uses the LoginFragment.java file
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

    /*private void startConnectingFragment()
    {
        // Start a connecting fragment so that the connecting message appears and disappears
        // as hoped for


    }*/


    // Removes all active fragments from the application
    // Generally called when a server disconnection occurs
    private void removeAllFragments()
    {
        Log.i("MainActivity", "Called RemoveAllFragments");
        for (Fragment fragment:getSupportFragmentManager().getFragments())
        {

            if(fragment != null)
            {
                getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            }
        }
    }

    // Check if the chat window with the name chatName is currently open
    // Returns the chatwindow fragment with the specified name
    // Returns null if the fragment isn't found
    ChatWindowFragment getChatWindowFragment(String chatName)
    {
        Log.i("MainActivity", "Called getChatWindowFragment");

        for (Fragment fragment: getSupportFragmentManager().getFragments())
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
        String tag = "sendChatMsgToServer";

        JSONObject json = new JSONObject();

        try
        {
            json.put("recipient", chatName);
            json.put("content"  , content);
            json.put("type", _GeneralMessageSendRequest);

        } catch (JSONException e)
        {
            e.printStackTrace();
            return false;
        }

        String message = json.toString();


        Log.i(tag, "Sending chat message(): " + message);
        MessageAck ack = sendMessageToServer(json, _GeneralMessageSendRequest);

        if (ack != null )
        {
            long status = ack.getStatus();

            if (status == _GeneralMessageSentSuccessfullyCode)
            {
                Log.i(tag,"General Message \"" + content + "\" sent");
                return true;
            }
            else
            {
                Log.i(tag,"General message \"" + content + "\" not sent, got status" + status);
                return false;
            }
        }
        else
        {
            Log.i(tag,"Error, Ack was null for general message sent");
            return false;
        }

    }

    // Returns the status code of a message if it was found in the list of message confirmations
    // Returns -1 if the message was never received
    private MessageAck waitUntilMessageAcked(long messageID, long timeToWaitMS)
    {
        String tag = "waitUntilMessageAcked";
        Log.i(tag, "Starting wait for message ID: " + messageID);
        // Calc time that we will timeout
        long timeOutExpiredTimeMS = System.currentTimeMillis() + timeToWaitMS;


        while (1 == 1)
        {
            //Log.i(tag, "Waiting for message");

            long waitMs = timeOutExpiredTimeMS - System.currentTimeMillis();

            if (waitMs <= 0)
            {
                Log.i(tag, "Reached timeout after waiting for messaged ID: " + messageID);
                return null;
            }

            // Check if we got a message acknowledgement for the messageID
            if (_AllMessageConfirmations.size() > 0)
            {
                //Log.i(tag, "MESSAGE ACK stack > 0");

                Iterator<MessageAck> iter = _AllMessageConfirmations.iterator();

                while (iter.hasNext())
                {
                    MessageAck msgAck = iter.next();

                    if (msgAck != null)
                    {
                        long currentID = msgAck.getID();

                        //Log.i(tag, "currentID is " + currentID + " with status " + msgAck.getStatus());

                        // If you we find the right message ack, remove it and return its status
                        if (currentID == messageID) {
                            Log.i(tag, "Found ack for messageID " + messageID + " with status " + msgAck.getStatus());
                            iter.remove();
                            return msgAck;
                        }
                    }

                }
            }
        }
    }


    /**********************************************************************************************/
    /*                           Websocket and Server Connection Functions                        */
    /**********************************************************************************************/

    // Converts a host url to the Java URI object
    // Helper function for setting up a websocket
    private void setupServerURI(String hostString)
    {
        Log.i("SetupServerURI", "Setting up URI for: " + hostString);

        try
        {
            _ServerURI = new URI(_MainHost);
            Log.i("SetupServerURI", "Setting up URI succeeded for: " + hostString);
        }
        catch (URISyntaxException e)
        {
            Log.i("SetupServerURI", "Setting up URI failed for: " + hostString);
            e.printStackTrace();
            return;
        }

    }

    private void setupWebSocketConnection(String hostString)
    {
        // Set up the URI for the connection
        setupServerURI(hostString);

        // Create new websocket object
        _WSClient = new ChatWebSocket(_ServerURI, this);
    }



    // Prompt WS client to connect to the server
    private void callWSConnect()
    {
        String tag = "callWSConnect";

        Log.i(tag, "WSConnect() with _ServerRetries at " + _HostRetries);

        if (_HostRetries >= _MaxHostRetries)
        {
            Log.i(tag, "Max retries reached for current host with index " + _CurrentServerHostIndex);

            // Try another server host
            _CurrentServerHostIndex++;

            Log.i(tag, "Next host index is: " + _CurrentServerHostIndex);

            // Reset server host index if necessary
            if (_CurrentServerHostIndex > _ServerHosts.size() - 1)
            {
                Log.i(tag, "Resetting host index to 0");
                _CurrentServerHostIndex = 0;
            }

            // Set up a connection to the new host
            String nextHost = _ServerHosts.get(_CurrentServerHostIndex);

            Log.i(tag, "Trying backup host: " + nextHost);

            // Set up the web socket connection with the host we chose
            setupWebSocketConnection(nextHost);

            // Reset number of times we've tried to connect to this host
            _HostRetries = 0;
        }

        Log.i(tag, "Calling _WSClient.connect()");
        // Attempt to connect to the server
        _WSClient.connect();
    }

    // Called by WebSocketClient when it has connected to the server
    public void onServerConnect()
    {
        String tag = "onServerConnect";
        Log.i(tag, "In function");

        // Indicate to everyone that we're connected to the server
        _WSConnected = true;

        // Reset number of server retries
        _HostRetries = 0;

        // Register current user with the server if necessary
        //if(!_UserHasRegisteredBefore)
        if (!_UserIsRegistered)
        {
            removeAllFragments();
            Log.i(tag, "First time user, prompting the login fragment");
            startLoginFragment();
        }
        /*else
        {
            Log.i(tag, "Skipping Login Fragment prompting user registration again");

            if (registerUserAgain(1) == false)
            {
                removeAllFragments();
                startLoginFragment();
            }
        }*/

        // Set the text in the connected layout to "connected!"
        setConnectingLayout(View.VISIBLE, "Connected!");
        //runOnUiThread(_SetConnectedText);

        // Set screen to touchable
        //setScreenTouchability(true);

        Log.i(tag, "Got past login fragment");

        // Wait for a few seconds and then make the connecting layout view disappear
        RemoveConnectingTextAfterDelay(2000);


    }

    // Called by the websocket client when the server disconnects
    public void onServerDisconnect()
    {
        Log.i("MainActivity", "Called OnServerDisconnect");

        _WSConnected = false;

        // Update number of retries
        _HostRetries++;

        // Set connected attribute to "connecting"
        setConnectingLayout(View.VISIBLE, "Connecting...");
        //runOnUiThread(_SetConnectingText);

        // Disable the screen from being touched by the user
        //setScreenTouchability(false);

        // Remove fragments from the display
        //removeAllFragments();

        // User is no longer registered when chats disconnect
        _UserIsRegistered = false;

        // Try reconnecting to the server
        _WSClient = new ChatWebSocket(_ServerURI, this);
        callWSConnect();

    }



    // Sets the screen to touchable or untouchable based on the argument
    private void setScreenTouchability(final boolean makeTouchable)
    {
        String tag = "setScreenTouchability";
        Log.i(tag, "in setScreenTouch with boolean " + makeTouchable);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = getWindow().getDecorView().getRootView();
                view.setEnabled(makeTouchable);
            }
        });
    }


    /**********************************************************************************************/
    /*                           User Registration Functions                                      */
    /**********************************************************************************************/

    // Register current client in chat server using the arguments passed to the function
    // Returns the status of the register code received, or a register failed code if
    // no response was received from the server
    public long registerUser(String username, String password, boolean doAuthentication)
    {
        String tag = "RegisterUser()";

        JSONObject json = new JSONObject();

        try
        {
            json.put("type",     _RegisterRequest);
            json.put("username", username);
            json.put("password", password);

            json.put("auth", doAuthentication);

        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        String message = json.toString();


        Log.i("MainActivity", "Sending registration message(): " + message);
        MessageAck ack = sendMessageToServer(json, _RegisterRequest);

        if (ack != null )
        {
            long status = ack.getStatus();

            if (status == _RegistrationApprovedCode)
            {
                _RegisterWithAuthentication = doAuthentication;
                _CurrentPassword            = password;
                _CurrentUser                = username;
                _UserHasRegisteredBefore    = true;

                Log.i(tag,"Registration approved");
            }
            else
            {
                Log.i(tag,"Registration not approved, got status " + status);
            }

            return status;
        }
        else
        {
            Log.i(tag,"Error, ack was null for Registration ");
            return _NoServerResponseCode;
        }
    }

    /*// Attempts to register a user that has already been registered in the system
    // up to <timesToTry> times
    private boolean registerUserAgain(int timesToTry)
    {

        String tag = "RegisterUserAgain";
        for (int i = 0; i < timesToTry ; i++)
        {
            Log.i(tag, "Attempt " + i + " to Re-Register User: " + _CurrentUser);
            long status = registerUser(_CurrentUser, _CurrentPassword, _RegisterWithAuthentication);

            if (status == _RegistrationApprovedCode)
            {
                Log.i(tag, "Re-Registration Success");
                return true;
            }
            else if (status == _UserAlreadyRegisteredCode)
            {
                Log.i(tag, "Previous username " + _CurrentUser + " has been taken");
                return false;
            }
        }

        return false;
    }*/


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
                    if (status == _RegistrationApprovedCode)
                    {
                        Log.i(tag, "Registration successful");

                        _UserIsRegistered = true;

                        // Get the token from the message
                        _UserToken = (String)jsonObject.get("token");

                        /*// Get the list of backups in case this host goes down
                        org.json.simple.JSONArray jsonArray = (org.json.simple.JSONArray)jsonObject.get("backupList");
                        String[] backups = new String[jsonArray.size()];

                        // Add all backups to our list of hosts that aren't already
                        // in the list
                        for(int i = 0; i < jsonArray.size(); i++)
                        {
                            String backupHost = (String)jsonArray.get(i);

                            //Log.i(tag, "current backupHost:" + backupHost);

                            if(!_ServerHosts.contains(backupHost))
                            {
                                _ServerHosts.add(backupHost);
                            }
                        }*/


                    }

                    // If user is already registered
                    else if (status == _UserAlreadyRegisteredCode)
                    {
                        Log.i(tag, "Error, username was already registered");
                        // Tell user to pick a new username
                    }

                    // if told to connect to a different server
                    // -search for the index of the host specified
                    // -set that index as the currentHostIndex
                    // -setup WebSocket connection with the new host
                    // -set hostRetries to 0
                    // -call onServerDisconnected

                    else
                    {
                        Log.i(tag, "Error, status is " + status);
                    }


                    long messageID = (long)jsonObject.get(_messageIDField);
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

                    long messageID = (long)jsonObject.get(_messageIDField);

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

                            if(!contact.equals(_CurrentUser) && !contact.equals(""))
                            {
                                contacts[i] = (String) jsonArray.get(i);
                            }
                        }

                        Log.i(tag, "Converting to array");
                        _ContactList = new ArrayList(Arrays.asList(contacts));
                    }

                    long messageID = (long)jsonObject.get(_messageIDField);
                    _AllMessageConfirmations.add(new MessageAck(messageID, status));

                    break;
                }

                case _RandomMessageResponse:
                {
                    Log.i(tag, "Got Random Message Response");
                    if (status == _RandomMessageSuccess)
                    {
                        Log.i(tag, "Success, got success code for random message");
                    }
                    else if (status == _RandomMessageError)
                    {
                        Log.i(tag, "Error, got error code for random message");
                    }
                    else
                    {
                        Log.i(tag, "Error, unknown random message status " + status);
                    }

                    long messageID = (long)jsonObject.get(_messageIDField);
                    _AllMessageConfirmations.add(new MessageAck(messageID, status, message));

                    break;
                }

                case _GeneralMessageSendResponse:
                {
                    Log.i(tag, "Got message send response");

                    if(status == _GeneralMessageSentSuccessfullyCode)
                    {
                        //Log.i(tag, "Adding to message confirmations");
                        //_MessageSentConfirmations.add(_SingleMessageResponseExample);

                    }

                    long messageID = (long)jsonObject.get(_messageIDField);
                    _AllMessageConfirmations.add(new MessageAck(messageID, status));

                    break;

                }

                /*case "RandomMessageRecv":
                {
                    Log.i("RandomMessageRecv", "Random Message Received");

                    if (status == 200 || status == 201)
                    {
                        String content      = (String) jsonObject.get("content");
                        String sender       = (String) jsonObject.get("sender");
                        //String chatName     = (String) jsonObject.get("chatname");
                        //boolean isGroupChat = (boolean) jsonObject.get("groupchat");

                        String chatName = sender;
                        boolean isGroupChat = false;

                        // Add a new chat for this chatName if it doesn't exist yet
                        if(_Chats.get(chatName) == null)
                        {
                            AddChat(chatName, isGroupChat);
                        }

                        // Add the new message to our list of received messages
                        Message newMessage = new Message(content, new User(sender), System.currentTimeMillis());
                        ReceivedMessage newPair = new ReceivedMessage(chatName, newMessage);
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
                }
                break;*/

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
                        ReceivedMessage newPair = new ReceivedMessage(chatName, newMessage);
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

                case _ErrorResponse:
                {
                    Log.i(tag, "Received an error response" );

                    long messageID = (long)jsonObject.get(_messageIDField);


                    _AllMessageConfirmations.add(new MessageAck(messageID, status));



                    break;
                }


                default:
                {
                    Log.i(tag, "Error, unknown type: " + type);

                    long messageID = (long)jsonObject.get(_messageIDField);
                    _AllMessageConfirmations.add(new MessageAck(messageID, status));
                }
            }
        }
        catch (Exception e)
        {
            Log.i("Parsing", "Error " + e.getMessage());
        }

    }


    // Requests a contact list from the chat server
    // Returns the current contact list that the chat application has most recently saved
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
        MessageAck ack = sendMessageToServer(json, _ClientListRequest);

        // Return current contact list regardless of the result
        if (ack != null)
        {
            long status = ack.getStatus();

            if (status == _ClientListProvidedCode)
            {
                Log.i("MainActivity", "Client list acked");
                return _ContactList;
            }
            else
            {
                Log.i("MainActivity", "Error, clientlist response code was " + status);
                return _ContactList;
            }
        }
        else
        {
            Log.i("MainActivity", "Client list ack not received");
            return _ContactList;
        }


    }

    // Checks for new messages for a given chat using main's MessagesReceivedList
    // Adds any messages it finds for the chat to the chat's chat history, and removes
    // it from the received messages list
    public void checkForNewMessages(String chatName)
    {
        Log.i("ChatWindowFragment", "in checkForNewMessages()");

        // Get the chat's chatHistory
        List<Message> chatHistory = _Chats.get(chatName).getChatHistory();

        if (chatHistory == null)
        {
            Log.i("ChatWindowFragment", "Error chat history for " + chatName + " is null");
        }

        Iterator<ReceivedMessage> iter = _MessagesReceived.iterator();

        // Check if we've any received messages for this chat
        while (iter.hasNext())
        {
            ReceivedMessage m = iter.next();

            String messageChatName = m.getChatname();

            Log.i("checkForNewMessages()", "requested chatname is " + chatName+ " and current chatname is " + messageChatName);

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

    // Attempts to register a chat with the server
    // Returns true automatically for single chats, since they don't need to be registered,
    // or group chats that received a groupChatApproved Code
    // Returns false if the chat registration was not approved by the server
    public boolean sendChatRegistrationToServer(List<String> contactsToAdd, String chatName)
    {
        String tag = "SendChatRegisToServer";

        // Single chats don't need to be registered to the server
        if (contactsToAdd.size() == 1)
        {
            return true;
        }

        // Otherwise, send a group chat
        JSONObject json = new JSONObject();

        try
        {

            JSONArray jsonArrayBetter =  new JSONArray();

            for (String contact: contactsToAdd)
            {
                jsonArrayBetter.put(contact);
            }

            json.put("type"      , _GroupMessageInitRequest);
            json.put("recipients", (Object)jsonArrayBetter);
            json.put("chatname"  , chatName);

        }
        catch (JSONException e)
        {
            e.printStackTrace();
            return false;
        }

        String message = json.toString();

        Log.i(tag, "Sending group init message(): " + message);
        MessageAck ack = sendMessageToServer(json, _GroupMessageInitRequest);

        if (ack != null )
        {
            long status = ack.getStatus();

            if (status == _GroupChatCreationApprovedCode)
            {
                Log.i(tag,"Group Chat creation approved");
                return true;
            }
            else
            {
                Log.i(tag,"Group Chat creation not approved, got status" + status);
                return false;
            }
        }
        else
        {
            Log.i(tag,"Error, Ack was null for Group Chat creation");
            return false;
        }
    }

    // Waits until the web socket client is connected, and then sends a message
    private MessageAck sendMessageToServer(JSONObject json, String msgType)
    {

        String tag = "sendMessageToServer";

        try
        {
            // Put the current ID in the json string
            json.put(_messageIDField, _currentMessageID);

            // Add a token to the request if it's not a register request
            if (!msgType.equals(_RegisterRequest))
            {
                json.put("token", _UserToken);
            }
        }
        catch (Exception e)
        {
            return null;
        }

        String message = json.toString();

        Log.i(tag, "in sendMessageToServer() for message " + message);

        // Loop until we are connected to the server, then send the message
        while (_WSClient == null || _WSConnected != true);
        _WSClient.send(message);

        // Wait up to 2 seconds for server to respond to our message
        MessageAck ack = waitUntilMessageAcked(_currentMessageID, 2000);

        // Check if our token is still valid and retrieve a new one if necessary
        if (ack != null && ack.getStatus() == _TokenNotAuthenticErrorCode)
        {
            Log.i(tag, "Received TokenNotAuthenticErrorCode");

            // Keep trying to register the user again until we are successful
            long registerStatus;

            do
            {
                Log.i(tag, "Attempting to retrieve token from server again");
                registerStatus = registerUser(_CurrentUser, _CurrentPassword, _RegisterWithAuthentication);
                Log.i(tag, "Got Registration code: " + registerStatus);

            } while(registerStatus != _RegistrationApprovedCode);
        }

        // Update message ID counter
        _currentMessageID++;

        // Reset our message ID counter if necessary
        if (_currentMessageID > _MaxMessageID)
        {
            _currentMessageID = 0;
        }

        return ack;
    }

    // Updates a chat with name <chatname> in the chat list and changes its preview message to
    // <content>
    public void updateChatMessagePreviewAndNotification(String chatName, String content, boolean isSent)
    {
        Log.i("updatePreview()", "in updateChatMessage Preview");

        String newPreviewMessage = "";

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

    // Sets the chats notified attribute for the chatname specified in our chat list,
    // The chats notified attribute indicating whether or not a new message is waiting in the chat
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
