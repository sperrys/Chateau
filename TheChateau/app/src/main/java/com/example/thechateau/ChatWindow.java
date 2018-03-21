package com.example.thechateau;

import android.os.*;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatWindow extends AppCompatActivity {

    private RecyclerView       _MessageRecycler;
    private MessageListAdapter _MessageAdapter;

    private List<Message> _MessageList = new ArrayList<Message>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);


        User    user1    = new User("User1");
        User    user2    = new User("User2");
        Message message1 = new Message("First message", user1, System.currentTimeMillis());
        Message message2 = new Message("Second message", user1, System.currentTimeMillis());
        Message message3 = new Message("First Received Message", user2, System.currentTimeMillis());


        _MessageList.add(message1);

        _MessageList.add(message2);
        _MessageList.add(message3);


        _MessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);
        _MessageAdapter  = new MessageListAdapter(this, _MessageList);
        _MessageRecycler.setLayoutManager(new LinearLayoutManager(this));


        _MessageRecycler.setAdapter(_MessageAdapter);

    }



}
