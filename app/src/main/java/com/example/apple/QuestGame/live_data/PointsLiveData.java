package com.example.apple.QuestGame.live_data;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class PointsLiveData extends ViewModel {

    private final MutableLiveData<String> selected = new MutableLiveData<>();

    public void select(String item) {
        selected.setValue(item);
    }

    public LiveData<String> getSelected() {
        return selected;
    }
}
