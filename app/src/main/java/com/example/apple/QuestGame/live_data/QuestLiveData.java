package com.example.apple.QuestGame.live_data;

import android.arch.lifecycle.MutableLiveData;

import com.example.apple.QuestGame.models.Quest;

import java.util.ArrayList;
import java.util.HashSet;

public class QuestLiveData {
    public static MutableLiveData<ArrayList<Quest>> selected = new MutableLiveData<>();

}
