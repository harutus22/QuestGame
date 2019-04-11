package com.example.apple.QuestGame.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.adapters.quest_view_holder.QuestHolder;
import com.example.apple.QuestGame.models.Quest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class QuestViewAdapter extends RecyclerView.Adapter<QuestHolder> {

    private List<Quest> data = new ArrayList<>();
    private OnItemSelectedListener onItemSelectedListener;
    private boolean passed;

    public QuestViewAdapter() {
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    private QuestHolder.OnItemClickListener onItemClickListener = new QuestHolder.OnItemClickListener() {
        @Override
        public void onItemClick(int adapterPosition) {
            if (onItemSelectedListener != null) {
                onItemSelectedListener.onItemSelected(data.get(adapterPosition));
            }
        }
    };

    @NonNull
    @Override
    public QuestHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        QuestHolder questHolder = new QuestHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.quest_view_layout, viewGroup, false));
        questHolder.setOnItemClickListener(onItemClickListener);

        return questHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull QuestHolder questHolder, int i) {
        Quest quest = data.get(i);
        questHolder.getTitle().setText(quest.getName());
        questHolder.getDescription().setText(quest.getDescription());
        questHolder.getReward().setText("Reward: " + String.valueOf(quest.getReward()));
        questHolder.getQuestImage().setImageBitmap(getFile(quest.getAvatar()));
    }

    public List<Quest> getData() {
        return data;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(List<Quest> data) {
        this.data = data;
    }

    public void addData(Quest quest) {
        data.add(quest);
        notifyItemInserted(data.size() - 1);
    }

    public interface OnItemSelectedListener {
        void onItemSelected(Quest quest);
    }

    private Bitmap getFile(String imageName) {
        String photoPath = Environment.getExternalStorageDirectory() + "/" + imageName + ".png";
        BitmapFactory.decodeFile(photoPath);
        return BitmapFactory.decodeFile(photoPath);
    }


}
