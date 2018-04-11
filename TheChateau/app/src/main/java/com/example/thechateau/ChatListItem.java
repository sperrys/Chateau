package com.example.thechateau;

/**
 * Created by russgomez on 4/11/18.
 */

public class ChatListItem {
    public String chatName;
    public String previewMessage;


    public ChatListItem(){
        super();
    }

    public ChatListItem(String chatName, String previewMessage) {
        super();
        this.chatName       = chatName;
        this.previewMessage = previewMessage;
    }
}
