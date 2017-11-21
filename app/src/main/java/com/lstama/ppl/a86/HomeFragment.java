package com.lstama.ppl.a86;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment implements View.OnClickListener{

    Button sendButton;

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        sendButton = (Button) view.findViewById(R.id.button_send_location);
        sendButton.setOnClickListener(this);
        return view;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView home_text = (TextView) getView().findViewById(R.id.home_text);
        home_text.setText(this.getArguments().getString("message"));
        getActivity().setTitle(R.string.title_home);
    }

    @Override
    public void onClick(View v) {
        MainActivity activity = (MainActivity) getActivity();
        // Now you can contact your activity through activity e.g.:
        sendButton.setEnabled(false);
        String email = this.getArguments().getString("email");
        String hashedPassword = this.getArguments().getString("hashed_password");
        activity.sendCurrentLocation(email, hashedPassword);
        sendButton.setEnabled(true);
    }
}
