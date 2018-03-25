package com.example.thechateau;

import android.content.Intent;
import android.os.*;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ChatWindow extends AppCompatActivity {

    private RecyclerView       _MessageRecycler;
    private MessageListAdapter _MessageAdapter;

    private List<Message> _MessageList;

    private String   _tag = "ChatWindow";
    private String   _chatName;

    private TextView _chatNameTextView;
    private Button   _sendButton;
    private EditText _sendMessageText;
    private User     _currentUser = new User(MainActivity.getCurrentUser());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);

        // Retrieve the chatName given to us by the calling intent
        Intent intent = getIntent();
        _chatName = intent.getStringExtra("chatName");

        Log.i(_tag, "Got chatName from intent: " + _chatName);

        /************************************/
        /* Display Chat Name in Chat Window */
        /************************************/
        _chatNameTextView = findViewById(R.id.chatName);
        _chatNameTextView.setText(_chatName);

        /************************************/
        /*     Retrieve other variables     */
        /************************************/

        // Get the chat history from the main activity
        _MessageList = MainActivity.getChatHistory(_chatName);

        // Get sendMessageText view (represents text user wants to send)
        _sendMessageText = findViewById(R.id.edittext_chatbox);

        /************************************/
        /*       Set up Send Button       */
        /************************************/
        // When clicked, it sends text found in the _sendMessageText object onto the screen
        _sendButton = findViewById(R.id.button_chatbox_send);
        _sendButton.setOnClickListener(   new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {

                Log.i(_tag, "Send Button clicked");

                // Get string from text box
                String sendString = _sendMessageText.getText().toString();

                // Make new message object
                Message newMessage = new Message(sendString, _currentUser, System.currentTimeMillis());

                // Add Message to message list and update
                _MessageList.add(newMessage);
                _MessageAdapter.notifyDataSetChanged();

                // Clear the text box
                _sendMessageText.getText().clear();

                // Move the current chat to most recently sent in Main's chat list
                //((MainActivity)getParent()).moveChatToTop(_chatName);
            }
        });

        /* Add Sample Messages to List */
        //User    user2    = new User("User2");
        //Message message1 = new Message("First message", _currentUser, System.currentTimeMillis());
        //Message message2 = new Message("Second message", _currentUser, System.currentTimeMillis());
        //Message message3 = new Message("First Received Message", user2, System.currentTimeMillis());

        //_MessageList.add(message1);
        //_MessageList.add(message2);
        //_MessageList.add(message3);
        //_MessageList.add(message3);
        //_MessageList.add(message3);


        /************************************/
        /*       Set up recycler view       */
        /************************************/

        _MessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);
        _MessageRecycler.setLayoutManager(new LinearLayoutManager(this));

        // Initialize an adapter that can adapt messages in the message list
        // And set it as the recycler view's adapter
        _MessageAdapter  = new MessageListAdapter(this, _MessageList);
        _MessageRecycler.setAdapter(_MessageAdapter);

    }

}
