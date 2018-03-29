package com.example.thechateau;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by russgomez on 3/19/18.
 */

public class MessageListAdapter extends RecyclerView.Adapter {
    private Context       _Context;
    private List<Message> _MessageList;
    private String        _CurrentUser;

    // Flags for formatting the timestamp displayed next to a message
    private int _dateFlags = DateUtils.FORMAT_SHOW_TIME;

    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 1;
    private static final int VIEW_TYPE_MESSAGE_SENT     = 2;

    public MessageListAdapter(Context context, List<Message> messageList, String currentUser) {
        _Context     = context;
        _MessageList = messageList;
        _CurrentUser = currentUser;

        Log.i("MessageListAdapter", "Here");
    }

    @Override
    public int getItemCount()
    {
       return  _MessageList.size();
    }

    @Override
    public int getItemViewType(int position) {

        Log.i("getItemViewType", "Here");
        // Get the message
        Message message = (Message) _MessageList.get(position);
        String sender = message.getSender().getName();

        Log.i("getItemViewType", "Sender is " + sender + ", Current is " + _CurrentUser);

        // If message sender is current user, it's a sent message
        // Otherwise, it's a received message
        if(sender.equals(_CurrentUser))
        {
            return VIEW_TYPE_MESSAGE_SENT;
        }
        else
        {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    // Inflates the appropriate layout based on the view type
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Log.i("onCreateViewHolder", "Here");
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT)
        {
            Log.i("onCreateViewHolder", "sent message");
            view = LayoutInflater.from(parent.getContext())
                                 .inflate(R.layout.item_message_sent, parent, false);

            return new SentMessageHolder(view);

        }
        else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED)
        {
            Log.i("onCreateViewHolder", "received message");
            view = LayoutInflater.from(parent.getContext())
                                 .inflate(R.layout.item_message_received, parent, false);

            return new ReceivedMessageHolder(view);
        }

        return null;
    }

    // Passes the message to a UI so contents can be bound to a UI
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {


        Log.i("onBindViewHolder", "Here");
        Message message = _MessageList.get(position);

        switch(holder.getItemViewType())
        {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;

            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;

        }

    }

    /***************************************************************/
    /*   Private Class Definitions of View Holders for Messages    */
    /***************************************************************/

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {


        TextView messageText, timeText, nameText;
        ImageView profileImage;

        ReceivedMessageHolder(View itemView) {

            super(itemView);

            messageText  = (TextView)  itemView.findViewById(R.id.text_message_body);
            timeText     = (TextView)  itemView.findViewById(R.id.text_message_time);
            nameText     = (TextView)  itemView.findViewById(R.id.text_message_sender);
            //profileImage = (ImageView) itemView.findViewById(R.id.message_icon);
        }

        void bind(Message message) {

            messageText.setText(message.getMessage());

            // Format the stored timestamp into a readable String
            Long timeStamp = message.getTimeCreated();
            String timeStampString = DateUtils.formatDateTime(_Context, timeStamp, _dateFlags);
            timeText.setText(timeStampString);

            nameText.setText(message.getSender().getName());

            // Insert the profile image from the URL into the ImageView.
            //Utils.displayRoundImageFromUrl(mContext, message.getSender().getProfileUrl(), profileImage);
        }

    }

    // Same as Received MessageHolder, but without nameText and profile Image
    private class SentMessageHolder extends RecyclerView.ViewHolder {


        TextView messageText, timeText;

        SentMessageHolder(View itemView) {
            super(itemView);

            messageText  = (TextView)  itemView.findViewById(R.id.text_message_body);
            timeText     = (TextView)  itemView.findViewById(R.id.text_message_time);

        }

        void bind(Message message) {

            messageText.setText(message.getMessage());

            // Format the stored timestamp into a readable String
            Long timeStamp = message.getTimeCreated();
            String timeStampString = DateUtils.formatDateTime(_Context, timeStamp, _dateFlags);
            timeText.setText(timeStampString);

            // Insert the profile image from the URL into the ImageView.
            //Utils.displayRoundImageFromUrl(mContext, message.getSender().getProfileUrl(), profileImage);
        }

    }
}

