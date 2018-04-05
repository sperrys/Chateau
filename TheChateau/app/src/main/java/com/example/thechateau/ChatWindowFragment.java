package com.example.thechateau;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class ChatWindowFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_CHATNAME = "chatName";
    //private static final String ARG_PARAM2 = "param2";

    private String _ChatName;
    private RecyclerView       _MessageRecycler;
    private MessageListAdapter _MessageAdapter;

    private List<Message> _MessageList;

    private String   _tag = "ChatWindowFragment";
    //private String   _chatName;

    private TextView _chatNameTextView;
    private Button   _sendButton;
    private Button   _backButton;
    private EditText _sendMessageText;
    private User     _currentUser;
    private TextView _InfoMessageTextView;

    private View _FragmentView;
    private Fragment thisFragment;


    private OnFragmentInteractionListener mListener;

    public ChatWindowFragment() {
        // Required empty public constructor
        _ChatName = "Default chat name";


    }



    public static ChatWindowFragment newInstance(String chatName) {

        ChatWindowFragment fragment = new ChatWindowFragment();

        // Add arguments to fragment instance
        Bundle args = new Bundle();
        args.putString(ARG_CHATNAME, chatName);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Make activity view invisible
        getActivity().findViewById(R.id.nonFragmentStuff).setVisibility(View.INVISIBLE);

        thisFragment = this;

        // Get the chat name argument
        if (getArguments() != null)
        {
            _ChatName = getArguments().getString(ARG_CHATNAME);
        }

        Log.i(_tag, "Got chatName: " + _ChatName);

        // Get current user
        _currentUser = new User(((MainActivity)getActivity()).getCurrentUser());


        /************************************/
        /* Display Chat Name in Chat Window */
        /************************************/
        _chatNameTextView = _FragmentView.findViewById(R.id.chatName);
        _chatNameTextView.setText(_ChatName);

        /************************************/
        /*     Retrieve other variables     */
        /************************************/

        // Get the chat history from the main activity
        _MessageList = MainActivity.getChatHistory(_ChatName);

        // Get sendMessageText view (represents text user wants to send)
        _sendMessageText     = _FragmentView.findViewById(R.id.edittext_chatbox);

        _InfoMessageTextView = _FragmentView.findViewById(R.id.InfoMessageText);

        /************************************/
        /*       Set up Send Button       */
        /************************************/
        // When clicked, it sends text found in the _sendMessageText object onto the screen
        _sendButton = _FragmentView.findViewById(R.id.button_chatbox_send);
        _sendButton.setOnClickListener(   new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {

                Log.i(_tag, "Send Button clicked");

                // Get string from text box
                String sendString = _sendMessageText.getText().toString();

                String infoString = "Sending \"" + sendString + "\" to recipient(s)";

                _InfoMessageTextView.setText(infoString);

                // Make new message object
                Message newMessage = new Message(sendString, _currentUser, System.currentTimeMillis());

                boolean isGroupChat = false; //(_MessageList.size() > 1);

                boolean messageSent = ((MainActivity) getActivity()).sendChatMessageToServer(_ChatName, sendString, isGroupChat);

                if (messageSent)
                {
                    Log.i(_tag, "Message sent through server");

                    // Add Message to message list and update
                    _MessageList.add(newMessage);
                    _MessageAdapter.notifyDataSetChanged();

                    // Clear the text box
                    _sendMessageText.getText().clear();

                    // Move the current chat to most recently sent in Main's chat list
                    ((MainActivity) getActivity()).moveChatToTop(_ChatName);

                    _InfoMessageTextView.setText("Message \"" + sendString + "\" sent successfully");
                }
                else {
                    Log.i(_tag, "Message not sent through server");

                    // ERROR message couldn't be sent
                    _InfoMessageTextView.setText("ERROR Message \"" + sendString + "\"couldn't be sent");

                }
            }
        });

        /************************************/
        /*       Set up Back Button       */
        /************************************/
        // When clicked, it sends text found in the _sendMessageText object onto the screen
        _backButton = _FragmentView.findViewById(R.id.backButton);
        _backButton.setOnClickListener(   new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {

                Log.i(_tag, "Back Button clicked");

                // Make parent activity visible again
                getActivity().findViewById(R.id.nonFragmentStuff).setVisibility(View.VISIBLE);

                //getActivity().findViewById(R.id.fragment_container).setVisibility(View.INVISIBLE);

                // End this fragment
                //getActivity().getSupportFragmentManager().beginTransaction().remove(thisFragment);
                getActivity().getSupportFragmentManager().popBackStack();


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

        _MessageRecycler = (RecyclerView) _FragmentView.findViewById(R.id.reyclerview_message_list);
        _MessageRecycler.setLayoutManager(new LinearLayoutManager(this.getContext()));

        // Initialize an adapter that can adapt messages in the message list
        // And set it as the recycler view's adapter
        _MessageAdapter  = new MessageListAdapter(this.getContext(), _MessageList, _currentUser.getName());
        _MessageRecycler.setAdapter(_MessageAdapter);

        // Check if there are pending messages sent to this chat from other users
        checkForNewMessages();
    }

    // Checks if there are any new received messages for the chat and updates the view
    // if any chats were received
    private void checkForNewMessages()
    {
        ((MainActivity)getActivity()).checkForNewMessages(_ChatName);
        _MessageAdapter.notifyDataSetChanged();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        _FragmentView = inflater.inflate(R.layout.fragment_chat_window, container, false);

        // Inflate the layout for this fragment
        return _FragmentView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;


    }



    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


}
