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


    //private String             _ContactsAddedString = "Contacts Added: ";
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


    private ArrayList<String>     _ContactsToAddList = new ArrayList<String>();
    private ListView              _ContactsToAddListView;
    private ArrayAdapter          _ContactsToAddAdapter;

    private ArrayList<String>     _AvailableContactList;
    private ListView              _AvailableContactsListView;
    private ArrayAdapter          _AvailableContactsAdapter;

    private String[]              _SampleAvailableContacts = {"Arnold", "Honnu", "Joey", "Johnny", "Alex", "Fernando", "Alfred", "Hitchcock", "Dennis", "Yorgen"};


    private String _Tag = "AddChatFragment";

    private OnFragmentInteractionListener mListener;

    public AddChatFragment() {
        // Required empty public constructor
    }

    /**********************************************************************************************/
    /*                                 Basic Setup Functions                                      */
    /**********************************************************************************************/

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Make activity view invisible
        getActivity().findViewById(R.id.nonFragmentStuff).setVisibility(View.INVISIBLE);

        // Set chatName to invisible until we need a group chat
        _ChatNameText.setVisibility(View.INVISIBLE);


        _SubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                 onSubmitChatButtonClick();
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


        // Update our list of available contacts by asking the server for a list of them
        UpdateAvailableContactsList();

        // Set up our two listviews
        setupAvailableContactListView();
        setupContactsToAddListView();
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
        _AvailableContactsListView   = _FragmentView.findViewById(R.id.ContactListView);
        _ContactsToAddListView = _FragmentView.findViewById(R.id.AddedContactsListView);
        _AddedContactTitleText = _FragmentView.findViewById(R.id.AddedContactTitle);

        // Inflate the layout for this fragment
        return _FragmentView;
    }

    /**********************************************************************************************/
    /*                                 Setup List View Functions                                  */
    /**********************************************************************************************/

    // Set up the list of AddedContacts
    private void setupContactsToAddListView()
    {
        // Set up List view so that clicking a contact removes it from the contacts to add list
        _ContactsToAddListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {

                String contact = (String)_ContactsToAddListView.getItemAtPosition(position);

                // Remove contact and notify the contacts-to-add list view
                _ContactsToAddList.remove(contact);
                _ContactsToAddAdapter.notifyDataSetChanged();

                // Remove the chat name field if necessary (in case removing the contact made the
                // the potential chat no longer be a group chat)
                if (_ContactsToAddList.size() <= 1)
                {
                    _ChatNameText.setVisibility(View.INVISIBLE);
                    _ErrorText.setText("");
                }

                // Update the available contact list in case the contact we removed is still
                // available to be added again
                UpdateAvailableContactsList();

            }
        });

        // Make and set the adapter for the contacts-to-add listview
        _ContactsToAddAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, _ContactsToAddList);
        _ContactsToAddListView.setAdapter(_ContactsToAddAdapter);

    }

    // Sets up the AvailableContactsListView
    private void setupAvailableContactListView()
    {
        // Set up List view so that clicking on an item in the list prompts adding the contact
        // to our potnetial chat
        _AvailableContactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {

                String contact = (String)_AvailableContactsListView.getItemAtPosition(position);

                onAddContactPrompted(contact);
            }
        });

        //Make and set the adapter for the AvailableContactsListView
        _AvailableContactsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, _AvailableContactList);
        _AvailableContactsListView.setAdapter(_AvailableContactsAdapter);
    }



    /**********************************************************************************************/
    /*                              Add Contact Functions                                         */
    /**********************************************************************************************/


    // Called whenever the user tries to add a contact by clicking on the "add contact" button
    // or by clicking on an item in the ContactsToAddListView
    private void onAddContactPrompted(String contact)
    {

        // Attempt to add the contact
        // Clear the error text if the contact was added succesfully
        // Otherwise error text will be set accordingly in AttemptAddContact
        if(AttemptAddContact(contact))
        {
            _ErrorText.setText("");
        }

        // If ContactsToAddList is big enough to be a group chat, make the chat name field
        // visible to the user so they can enter a name for it
        if(_ContactsToAddList.size() > 1)
        {
            _ErrorText.setText("Please Enter a Name For The New Group Chat");
            _ChatNameText.setVisibility(View.VISIBLE);
        }

        // Clear the contact text box to show that the contact was added
        _AddContactText.getText().clear();;
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
        if (!ContactExists(contact))
        {
            Log.i(_Tag,"ERROR: contact is not registered in our database");
            _ErrorText.setText("ERROR: contact \"" + contact + "\" is not registered in our database");
            return false;
        }

        // Check contact isn't already set to be added our list of contacts to add
        if(ContactAlreadyAdded(contact))
        {
            Log.i(_Tag,"ERROR: contact is already set to be added");
            _ErrorText.setText("ERROR: contact \"" + contact + "\" is already set to be added");
            return false;
        }

        // Add contacts to our contactList
        _ContactsToAddList.add(contact);

        // Update contacts to add list that is displayed to the user
        UpdateContactsToAddListView();

        // Remove all contacts in the added list from the available contacts list
        RemoveContactsToAddFromAvailableContactsList();

        return true;
    }




    /**********************************************************************************************/
    /*                      Contact List Housekeeping Functions                                   */
    /**********************************************************************************************/

    // Remove all contacts in the added list from the contact list displayed to the user
    private void RemoveContactsToAddFromAvailableContactsList()
    {
        for(String contact: _ContactsToAddList)
        {
            Log.i("removedAddedContactList", "Removing " + contact);
            _AvailableContactList.remove(contact);

        }

        UpdateAvailableContactsListView();
    }

    // Update the AvailableContactsList by getting the server's contact list
    // -Also, update the UI to show that available contacts changed
    private void UpdateAvailableContactsList()
    {
        // Get a list of available contacts from the server
        _AvailableContactList = new ArrayList<>(Arrays.asList(_SampleAvailableContacts));
        //_AvailableContactList = ((MainActivity)getActivity()).requestContactList();

        if (_AvailableContactList == null)
        {
            _AvailableContactList = new ArrayList<>();
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

        // Remove contacts in the ContactsToAddList from the list of available contacts
        RemoveContactsToAddFromAvailableContactsList();

    }

    // Updates the listview with the current available contacts
    private void UpdateAvailableContactsListView()
    {
        Log.i(_Tag, "in updateListView()");

        _AvailableContactsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, _AvailableContactList);

        //Log.i(_Tag, "made new adapter");

        _AvailableContactsListView.setAdapter(_AvailableContactsAdapter);

        //Log.i(_Tag, "set new adapter");
    }

    // Update the UI to show all contacts added
    private void UpdateContactsToAddListView()
    {
        //_ContactsToAddList.add(contact);
        _ContactsToAddAdapter.notifyDataSetChanged();

    }

    // Returns true if the contact is already in our contacts-to-add list
    private boolean ContactAlreadyAdded(String contact)
    {
        return _ContactsToAddList.contains(contact);
    }

    // Returns true if the contact is registered in the server's contact list
    private boolean ContactExists(String contact)
    {
        // Request contact list from server
        UpdateAvailableContactsList();

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

    /**********************************************************************************************/
    /*                                 Chat Submission Functions                                  */
    /**********************************************************************************************/

    private void onSubmitChatButtonClick()
    {
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


    // Attempts to register a chat with the server
    // Returns true if the chat was registered successfully
    // Returns false otherwise
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


        if (ChatNameAlreadyExists(chatName))
        {
            _ErrorText.setText("ERROR: Chat name already exists");
            _ChatNameText.getText().clear();
            return false;
        }

        // Attempt to register the new chat with the server and send the result
        boolean chatIsRegisteredWithServer = sendSubmissionToServer(chatName);

        return chatIsRegisteredWithServer;
    }

    // Attempts to register the new chat with the server
    // Returns true if the chat was registered successfully with the server
    // Returns false otherwise
    private boolean sendSubmissionToServer(String chatName)
    {

        boolean success = ((MainActivity)getActivity()).sendChatRegistrationToServer(_ContactsToAddList, chatName);

        if (success)
        {
            Log.i(_Tag, "Chat registration successful");

            // Return true if we successfully registered new chat
            _newChatName = chatName;

            return true;
        }
        else
        {
            Log.i(_Tag, "Chat registration failed");

            _ErrorText.setText("ERROR: chat name " + chatName + " could not be registered");

            return false;
        }
    }

    // Returns true if chatName already exists in the list of the user's chatnames
    private boolean ChatNameAlreadyExists(String chatName)
    {
        List<ChatListItem> chatNames = ((MainActivity)getActivity()).getChatList();

        ChatListItem item = ((MainActivity) getActivity()).getChatListItemWithChatName(chatNames, chatName);

        boolean containsName = item != null;

        return containsName;
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