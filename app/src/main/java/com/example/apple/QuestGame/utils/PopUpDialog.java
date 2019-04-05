package com.example.apple.QuestGame.utils;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.apple.QuestGame.R;


public class PopUpDialog extends DialogFragment {

    private TextView viewTitle, viewDescription;
    private String title;
    private String description;
    private Button start;
    private OnButtonClick onButtonClick;
    private boolean questStarted;

    public PopUpDialog(){}

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public Button getStart() { return start; }

    public void setOnButtonClick(OnButtonClick onButtonClick) {
        this.onButtonClick = onButtonClick;
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_window, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        if(title != null) {
            viewTitle.setText(title);
        }
        viewDescription.setText(description);
        if(questStarted){
            start.setVisibility(View.GONE);
        }
//        start.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onButtonClick.onClick(v);
//                change();
//                start.setVisibility(View.GONE);
//            }
//        });
    }

    @Override
    public void onStart() {
        super.onStart();
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClick.onClick(v);
                change();
            }
        });
    }

    private void initViews(View view){
        viewTitle = view.findViewById(R.id.pop_up_text_title);
        viewDescription = view.findViewById(R.id.pop_up_text_description);
        start = view.findViewById(R.id.pop_up_quest_start);
    }

    private void change(){
        questStarted = true;
    }

    public interface OnButtonClick{
        void onClick(View Button);
    }
}
