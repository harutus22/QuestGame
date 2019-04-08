package com.example.apple.QuestGame.adapters.quest_view_holder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.apple.QuestGame.R;

public class QuestHolder extends RecyclerView.ViewHolder {

    private ImageView questImage;
    private TextView title;
    private TextView description;
    private TextView reward;
    private OnItemClickListener onItemClickListener;

    public ImageView getQuestImage() { return questImage; }

    public TextView getTitle() { return title; }

    public TextView getDescription() { return description; }

    public TextView getReward() { return reward; }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(onItemClickListener != null){
                onItemClickListener.onItemClick(getAdapterPosition());
            }
        }
    };

    public QuestHolder(@NonNull View itemView) {
        super(itemView);
        questImage = itemView.findViewById(R.id.questImage);
        title = itemView.findViewById(R.id.questTitle);
        description = itemView.findViewById(R.id.questDescription);
        reward = itemView.findViewById(R.id.questReward);
        itemView.setOnClickListener(mOnClickListener);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(int adapterPosition);
    }

}
