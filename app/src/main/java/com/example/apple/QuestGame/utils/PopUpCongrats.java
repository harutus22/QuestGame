package com.example.apple.QuestGame.utils;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.apple.QuestGame.R;

public class PopUpCongrats extends DialogFragment {

    private TextView rewardView;
    private ImageView close;
    private String reward;

    public PopUpCongrats() {
    }

    public TextView getRewardView() {
        return rewardView;
    }

    public String getReward() {
        return reward;
    }

    public void setReward(String reward) {
        this.reward = reward;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_congrats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void initViews(View view) {
        rewardView = view.findViewById(R.id.pop_up_text_reward);
        close = view.findViewById(R.id.pop_close);
        rewardView.setText("+ " + reward);
    }
}

