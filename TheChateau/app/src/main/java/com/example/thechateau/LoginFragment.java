package com.example.thechateau;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class LoginFragment extends Fragment {

    //private MainActivity _MainActivity;
    private View         _FragmentView;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    EditText _PasswordEditText;
    EditText _UsernameEditText;
    Button   _LoginButton;
    TextView _InfoMessageText;


    public LoginFragment() {
        //_MainActivity = (MainActivity)getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }*/
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _LoginButton      = _FragmentView.findViewById(R.id.LoginButton);
        _UsernameEditText = _FragmentView.findViewById(R.id.UsernameEditText);
        _PasswordEditText = _FragmentView.findViewById(R.id.PasswordEditText);
        _InfoMessageText  = _FragmentView.findViewById(R.id.InfoMessageText);

        getActivity().findViewById(R.id.nonFragmentStuff).setVisibility(View.INVISIBLE);
        //_MainActivity.findViewById(R.id.nonFragmentStuff).setVisibility(View.INVISIBLE);

        _LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                String username, password;

                username = _UsernameEditText.getText().toString();
                password = _PasswordEditText.getText().toString();

                if (((MainActivity)getActivity()).registerUser(username, password))
                {
                    _InfoMessageText.setText("Registration successful");
                    getActivity().onBackPressed();
                }
                else
                {
                    _InfoMessageText.setText("ERROR, registration failed");

                    // Clear text info
                    _UsernameEditText.getText().clear();
                    _PasswordEditText.getText().clear();
                }
            }
        });


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        _FragmentView = inflater.inflate(R.layout.fragment_login, container, false);
        return _FragmentView;
    }

}
