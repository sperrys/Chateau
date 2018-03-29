package com.example.thechateau;

import java.util.ArrayList;

/**
 * Created by russgomez on 3/29/18.
 */

public class Chat {
    private String             _ChatName    = "";
    private boolean            _IsGroupChat = false;
    private ArrayList<String>  _ChatMembers = new ArrayList();
    private ArrayList<Message> _ChatHistory = new ArrayList();

    // Constructor for 1 on 1 chat
    public Chat(String chatName, String chatMember)
    {
        _ChatName = chatName;
        _ChatMembers.add(chatMember);
    }

    // Constructor for group chat
    public Chat(String chatName, ArrayList<String> chatMembers)
    {
        _ChatName    = chatName;
        _ChatMembers = chatMembers;

        if (_ChatMembers.size() > 1)
        {
            _IsGroupChat = true;
        }
    }

    public String getChatName()
    {
        return _ChatName;
    }

    public boolean addChatMember(String chatMember)
    {
        // Check if member already exists in the chat

        // If not, add the member to the chat

        // Update group chat variable if necessary
        if (_ChatMembers.size() > 1)
        {
            _IsGroupChat = true;
        }

        return true;
    }

    public void addMessage(Message message)
    {
        _ChatHistory.add(message);
    }


    public boolean isGroupChat()
    {
        return _IsGroupChat;
    }
}
