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
    private Button startBtn;
    private TextView description;
    private ImageView expandView;
    private OnItemClickListener onItemClickListener;
    private OnMinimizeListener onMinimizeListener;

    public ImageView getQuestImage() { return questImage; }

    public TextView getTitle() { return title; }

    public Button getStartBtn() { return startBtn; }

    public TextView getDescription() { return description; }

    public ImageView getExpandView() { return expandView; }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(onItemClickListener != null){
                onItemClickListener.onItemClick(getAdapterPosition());
            }
        }
    };

    private View.OnClickListener mOnMinClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(onMinimizeListener != null){
                onMinimizeListener.onItemClick(getAdapterPosition());
            }
        }
    };

    public QuestHolder(@NonNull View itemView) {
        super(itemView);
        questImage = itemView.findViewById(R.id.questImage);
        title = itemView.findViewById(R.id.questTitle);
        startBtn = itemView.findViewById(R.id.questStartButton);
        description = itemView.findViewById(R.id.questDescription);
        expandView = itemView.findViewById(R.id.questMinimize);
        expandView.setOnClickListener(mOnMinClickListener);
        itemView.setOnClickListener(mOnMinClickListener);
    }

    public void setmOnClickListener(View.OnClickListener mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
    }

    public void setmOnMinClickListener(View.OnClickListener mOnMinClickListener) {
        this.mOnMinClickListener = mOnMinClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(int adapterPosition);
    }

    public interface OnMinimizeListener{
        void onItemClick(int adapterPosition);
    }
}
