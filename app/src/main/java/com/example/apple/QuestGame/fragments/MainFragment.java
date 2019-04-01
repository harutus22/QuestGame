package com.example.apple.QuestGame.fragments;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.live_data.PointsLiveData;

public class MainFragment extends Fragment{

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private TextView mUsername;
    private TextView mPoints;
    private PointsLiveData model;


    public MainFragment() { }

    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = ViewModelProviders.of(getActivity()).get(PointsLiveData.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        init(view);
        setUserInfo();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void setUserInfo() {
            String name = getArguments().getString(ARG_PARAM1);
            String points = getArguments().getString(ARG_PARAM2);
            mUsername.setText(name);
            if(model.getSelected().getValue() == null){
                mPoints.setText(points);
            } else {
                model.getSelected().observe(this, new Observer<String>() {
                    @Override
                    public void onChanged(@Nullable String s) {
                        mPoints.setText(s);
                    }
                });
            }

    }

    private void init(View container){
        mUsername = container.findViewById(R.id.mainFragmentUsername);
        mPoints = container.findViewById(R.id.mainFragmentPoints);
    }

    @Override
    public void onStop() {
        super.onStop();
        String message = mPoints.getText().toString();
        model.select(message);
    }


}
