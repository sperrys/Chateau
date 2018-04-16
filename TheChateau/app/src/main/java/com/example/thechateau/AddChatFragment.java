package com.example.thechateau;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class AddChatFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private ArrayList<String>  _ContactsToAddList = new ArrayList<String>();
    private ListView           _ContactsAddedListView;
    private String             _ContactsAddedString = "Contacts Added: ";
    private String             _newChatName;

    private Button   _SubmitButton;
    private Button   _AddContactButton;
    private EditText _ChatNameText;
    private EditText _AddContactText;
    private TextView _ContactsAddedText;
    private TextView _ErrorText;
    private TextView _ContactTitleText;
    private TextView _AddedContactTitleText;
    private View     _FragmentView;

    private String[]              _SampleAvailableContacts = {"Arnold", "Honnu", "Joey", "Johnny", "Alex", "Fernando", "Alfred", "Hitchcock", "Dennis", "Yorgen"};
    private ArrayList<String>     _AvailableContactList;
    private ArrayAdapter          _ContactListAdapter;
    private ArrayAdapter          _AddedContactsAdapter;
    private ListView              _ContactListView;

    private String _Tag = "AddChatFragment";

    private OnFragmentInteractionListener mListener;

    public AddChatFragment() {
        // Required empty public constructor
    }





    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set chatName to invisible until we need a group chat
        _ChatNameText.setVisibility(View.INVISIBLE);

        // Make activity view invisible
        getActivity().findViewById(R.id.nonFragmentStuff).setVisibility(View.INVISIBLE);

        _SubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // If no contacts have been added yet, it's possible the user
                // entered a contact in the contact field without clicking Add Contact
                // If adding a contact fails (Error messages will be shown after
                // calling AttemptAddcContact)
                // Otherwise, continue and attempt to submit the new chat
                if (_ContactsToAddList.size() < 1)
                {
                    String contact = _AddContactText.getText().toString();

                    // Attempt to add a contact, assuming one is written on the add contact line
                    if(!AttemptAddContact(contact))
                    {
                        _ErrorText.setText("ERROR: Add at least one contact to create a new chat");
                        return;
                    }
                }

                boolean success = AttemptSubmission();

                if(success)
                {

                    Log.i(_Tag, "Chat was submitted successfully");

                    // Add new chat to our database of chats
                    ((MainActivity) getActivity()).AddChat(_newChatName, (_ContactsToAddList.size() > 1));

                    // Make new chat appear at top of user's list of chats
                    //((MainActivity) getActivity()).moveChatToTop(_newChatName);

                    // Go back to the activity
                    getActivity().onBackPressed();
                }

            }
        });

        _AddContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String contact = _AddContactText.getText().toString();

                // Run process for trying to add a contact
                onAddContactPrompted(contact);

            }
        });


        // Update our list of available contacts by asking the server
        UpdateContactList();

        setupContactListView();
        setupAddedContactListView();
    }

    // Set up the list of AddedContacts
    private void setupAddedContactListView()
    {
        // Set up List view so that clicking on an item in the list prompts adding the contact
        _ContactsAddedListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {

                String contact = (String)_ContactsAddedListView.getItemAtPosition(position);

                _ContactsToAddList.remove(contact);

                _AddedContactsAdapter.notifyDataSetChanged();

                UpdateContactList();

                if (_ContactsToAddList.size() <= 1)
                {
                    _ChatNameText.setVisibility(View.INVISIBLE);
                    _ErrorText.setText("");
                }

            }
        });


        // Make an adapter for the Chat List view and set it
        _AddedContactsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, _ContactsToAddList);

        // Set up Contact List View from UI
        _ContactsAddedListView.setAdapter(_AddedContactsAdapter);

    }

    // Sets up the ContactList View
    private void setupContactListView()
    {
        // Set up List view so that clicking on an item in the list prompts adding the contact
        _ContactListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {

                String contact = (String)_ContactListView.getItemAtPosition(position);

                onAddContactPrompted(contact);
            }
        });

        // Make an adapter for the Chat List view and set it
        _ContactListAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, _AvailableContactList);

        // Set up Contact List View from UI
        _ContactListView.setAdapter(_ContactListAdapter);
    }

    // Called whenever the add contact button or the list view is clicked to add a contact
    private void onAddContactPrompted(String contact)
    {

        if(AttemptAddContact(contact))
        {
            // Clear the error text if the contact was added succesfully
            _ErrorText.setText("");
        }

        if(_ContactsToAddList.size() > 1)
        {
            _ErrorText.setText("Please Enter a Name For The New Group Chat");
            _ChatNameText.setVisibility(View.VISIBLE);
        }

        // Clear the contact text field
        _AddContactText.getText().clear();;
    }

    // Update the contact list by getting the server's contact list
    // also updates the UI to show contacts that changed
    private void UpdateContactList()
    {
        // Get a list of available contacts from the server
        _AvailableContactList = new ArrayList<>(Arrays.asList(_SampleAvailableContacts));
        //_AvailableContactList = ((MainActivity)getActivity()).requestContactList();

        if (_AvailableContactList == null)
        {
            _AvailableContactList = new ArrayList<String>();
        }

        Log.i(_Tag, "Returned from request contactList");

        // Show that no contacts are available if the list is empty
        if (_AvailableContactList.size() < 1)
        {
            _ContactTitleText.setText("No Contacts Available");
        }
        else
        {
            _ContactTitleText.setText("Contacts");
        }

        // Remove Contacts in the added list from the list of available contacts
        removeAddedFromContactList();

        // Update list view to show contacts changed
        updateContactListView();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        _FragmentView = inflater.inflate(R.layout.fragment_add_chat, container, false);

        _SubmitButton      = _FragmentView.findViewById(R.id.SubmitButton);
        _AddContactButton  = _FragmentView.findViewById(R.id.AddContactButton);
        _ChatNameText      = _FragmentView.findViewById(R.id.ChatNameTextBox);
        _AddContactText    = _FragmentView.findViewById(R.id.AddContactTextBox);
        //_ContactsAddedText = _FragmentView.findViewById(R.id.ContactsAdded);
        _ErrorText         = _FragmentView.findViewById(R.id.ErrorMessage);
        _ContactTitleText  = _FragmentView.findViewById(R.id.ContactTitle);
        _ContactListView   = _FragmentView.findViewById(R.id.ContactListView);
        _ContactsAddedListView = _FragmentView.findViewById(R.id.AddedContactsListView);
        _AddedContactTitleText = _FragmentView.findViewById(R.id.AddedContactTitle);


        // Inflate the layout for this fragment
        return _FragmentView;
    }

    // Adds a contact to the contact list
    // If the contact name is empty, or the contact does not exist in the server,
    // or the contact has already been added to our contactsToAddList, the contact
    // isn't added and the function returns false
    // Otherwise adds the contact and returns true
    private boolean AttemptAddContact(String contact)
    {

        if (contact.equals(""))
        {
            Log.i(_Tag,"contact field is empty");
            _ErrorText.setText("ERROR: contact field is empty");
            return false;
        }

        // Check if contact is registered with the server
        if (!contactExists(contact))
        {
            Log.i(_Tag,"ERROR: contact is not registered in our database");
            _ErrorText.setText("ERROR: contact \"" + contact + "\" is not registered in our database");
            return false;
        }

        // Check contact isn't already set to be added our list of contacts to add
        if(contactAlreadyAdded(contact))
        {
            Log.i(_Tag,"ERROR: contact is already set to be added");
            _ErrorText.setText("ERROR: contact \"" + contact + "\" is already set to be added");
            return false;
        }

        // Add contacts to our contactList
        _ContactsToAddList.add(contact);

        // Update added list of contacts displayed to the user
        updateContactsAddedView();

        // Remove all contacts in the added list from the available contacts list
        removeAddedFromContactList();

        return true;
    }

    // Remove all contacts in the added list from the contact list displayed to the user
    private void removeAddedFromContactList()
    {
        for(String contact: _ContactsToAddList)
        {
            Log.i("removedAddedContactList", "Removing " + contact);
            _AvailableContactList.remove(contact);

        }

        updateContactListView();
    }
    // Updates the listview with the current available contacts
    private void updateContactListView()
    {
        Log.i(_Tag, "in updateListView()");

        _ContactListAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, _AvailableContactList);

        //Log.i(_Tag, "made new adapter");

        _ContactListView.setAdapter(_ContactListAdapter);

        //Log.i(_Tag, "set new adapter");
    }

    // Update the UI to show all contacts added
    // -Added a comma if necessary
    // -Add contacts's name to list
    // -Update the ContactsAdded View on the UI
    private void updateContactsAddedView()
    {
        //_ContactsToAddList.add(contact);
        _AddedContactsAdapter.notifyDataSetChanged();

        /*if (_ContactsToAddList.size() > 1) {
            _ContactsAddedString += ", ";
        }

        _ContactsAddedString += contact;

        _ContactsAddedText.setText(_ContactsAddedString);*/
    }

    // Returns true if the contact is already set to be added to our database
    private boolean contactAlreadyAdded(String contact)
    {
        return _ContactsToAddList.contains(contact);
    }

    // Returns true if the contact is registered in the server's contact list
    private boolean contactExists(String contact)
    {
        // Request contact list from server
        UpdateContactList();

        // Look through server's contact list and check if contact exists
        if (_AvailableContactList.contains(contact))
        {
            return true;
        }
        else
        {
            return false;
        }

    }

    // Attempts to register a chat with the server
    private boolean AttemptSubmission()
    {
        String chatName = _ChatNameText.getText().toString();

        /********************/
        /* Check for Errors */
        /********************/

        // Handle "chatName is empty" case
        if (chatName.equals(""))
        {
            // If chatName is empty for group chat, give an error
            if (_ContactsToAddList.size() > 1)
            {
                _ErrorText.setText("ERROR: You must have a chat name for group chats");
                return false;
            }

            // If it's a 1 on 1 chat, default chatName is name of person you're chatting with
            else
            {
                chatName = _ContactsToAddList.get(0);
            }
        }

        Log.i(_Tag, "_ContactList size is: " + _ContactsToAddList.size());
        Log.i(_Tag, "_ChatNameText is "      + chatName);


        // Give error if user tries to name a 1 on 1 chat
        /*if (_ContactsToAddList.size() == 1 && !chatName.equals(""))
        {
            _ErrorText.setText("ERROR: You can't name 1 on 1 chats ");
            _ChatNameText.getText().clear();
            return false;
        }*/

        if (chatNameAlreadyExists(chatName))
        {
            _ErrorText.setText("ERROR: Chat name already exists");
            _ChatNameText.getText().clear();
            return false;
        }

        // Attempt to register the new chat with the server and send the result
        boolean chatRegisteredWithServer = sendSubmissionToServer(chatName);

        return chatRegisteredWithServer;
    }

    // Returns true if chatName already exists in the list of the user's chatnames
    private boolean chatNameAlreadyExists(String chatName)
    {
        List<ChatListItem> chatNames = ((MainActivity)getActivity()).getChatList();

        ChatListItem item = ((MainActivity) getActivity()).getChatListItemWithChatName(chatNames, chatName);

        boolean containsName = item != null;

        return containsName;
    }


    private boolean sendSubmissionToServer(String chatName)
    {

        boolean success = ((MainActivity)getActivity()).sendChatRegistrationToServer(_ContactsToAddList, chatName);


        if (success)
        {
            Log.i(_Tag, "chat registration successful");

            // Return true if we successfully registered new chat
            _newChatName = chatName;

            return true;
        }
        else
        {
            Log.i(_Tag, "chat registration failed");

            _ErrorText.setText("ERROR: chat name " + chatName + " could not be registered");
            return false;
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}