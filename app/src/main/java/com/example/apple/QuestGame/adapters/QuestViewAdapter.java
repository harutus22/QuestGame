package com.example.apple.QuestGame.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.adapters.quest_view_holder.QuestHolder;
import com.example.apple.QuestGame.models.Quest;

import java.util.ArrayList;
import java.util.List;


public class QuestViewAdapter extends RecyclerView.Adapter<QuestHolder> {

    private List<Quest> data = new ArrayList<>();

    public QuestViewAdapter(){}

    @NonNull
    @Override
    public QuestHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        QuestHolder questHolder = new QuestHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.quest_view_layout, viewGroup, false));
//        questHolder.setmOnClickListener(OnItemClickListener);
//        questHolder.setmOnMinClickListener(OnItemRemoveClickListener);
        return questHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull QuestHolder questHolder, int i) {

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public interface OnItemSelectedListener {
        void onItemSelected(Quest quest);
    }

    public interface OnItemRemoveSelectedListener{
        void onItemRemoveSelected();
    }

    public List<Quest> getData() {
        return data;
    }
}
