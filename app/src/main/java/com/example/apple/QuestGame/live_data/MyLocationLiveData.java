package com.example.apple.QuestGame.live_data;

import android.arch.lifecycle.MutableLiveData;
import android.location.Location;

public class MyLocationLiveData {
    public static MutableLiveData<Location> myLocation = new MutableLiveData<>();
}
