package com.example.apple.QuestGame.live_data;

import android.arch.lifecycle.MutableLiveData;

import com.example.apple.QuestGame.models.Quest;

public class QuestLiveData {
    public static MutableLiveData<Quest> selected = new MutableLiveData<>();

}
