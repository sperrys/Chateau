package com.example.thechateau;

/**
 * Created by russgomez on 3/19/18.
 */

public class Message {

    String _Message;
    User   _Sender;
    long   _timeCreated;

    public Message(String msg, User sender, long timeCreated )
    {
        _Message     = msg;
        _Sender      = sender;
        _timeCreated = timeCreated;


    }

    public String getMessage() {
        return _Message;
    }

    public long getTimeCreated() {
        return _timeCreated;
    }

    public User getSender() {
        return _Sender;
    }



}




