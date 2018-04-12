package com.example.thechateau;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.LinkedList;

/**
 * Created by russgomez on 4/11/18.
 */

public class ChatListAdapter extends ArrayAdapter<ChatListItem> {

    Context context;
    int     layoutResourceId;
    LinkedList<ChatListItem> chatListItems = null;

    public ChatListAdapter(Context context, int layoutResourceId, LinkedList<ChatListItem> data) {
        super(context, layoutResourceId, data);


        this.layoutResourceId = layoutResourceId;
        this.context          = context;
        this.chatListItems = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row                  = convertView;
        ChatListItemHolder holder = null;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();

            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ChatListItemHolder();
            holder.chatNameView           = row.findViewById(R.id.chatNameTitle);
            holder.chatPreviewMessageView = row.findViewById(R.id.chatHistoryPreview);

            row.setTag(holder);
        }
        else
        {
            holder = (ChatListItemHolder)row.getTag();
        }


        ChatListItem chatListItem = chatListItems.get(position);

        Log.i("ChatListAdapter", "in getView, setting text of chat: " + chatListItem.chatName + " with message: " + chatListItem.previewMessage);
        holder.chatNameView.setText(chatListItem.chatName);
        holder.chatPreviewMessageView.setText(chatListItem.previewMessage);

        return row;


    }

    // Sets the preview of a given chatlist item
    public void setPreviewMessage(String chatName, String newPreview)
    {
        ChatListItem chatToChange = null;

        for(ChatListItem chat: chatListItems)
        {
            if(chat.chatName.equals(chatName))
                chatToChange = chat;
        }

        if (chatToChange != null)
        {
            Log.i("ChatListAdapter","setPrevMsg: Setting preview message");
            chatToChange.previewMessage = newPreview;
        }
    }


    static class ChatListItemHolder
    {
        TextView chatNameView;
        TextView chatPreviewMessageView;
    }
}
