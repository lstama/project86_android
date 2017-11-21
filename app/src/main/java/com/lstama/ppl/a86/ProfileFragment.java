package com.lstama.ppl.a86;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
     * A simple {@link Fragment} subclass.
     */
    public class ProfileFragment extends Fragment {


        public ProfileFragment() {
            // Required empty public constructor
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.fragment_profile, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            TextView email_text = (TextView) getView().findViewById(R.id.profile_email);
            email_text.setText(this.getArguments().getString("email"));
            TextView name_text = (TextView) getView().findViewById(R.id.full_name);
            name_text.setText(this.getArguments().getString("fullname"));
            getActivity().setTitle(R.string.title_profile);
        }

    }
