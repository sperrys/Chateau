package com.example.thechateau;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ChatWindow extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);



    }

    class Message {
        String message;
        User sender;
        long timeCreated;

    }

    class User {
        String name;
    }
}
