package com.example.apple.QuestGame.live_data;

import android.arch.lifecycle.MutableLiveData;

import com.google.android.gms.maps.model.LatLng;


public class MyLocationLiveData {
    public static MutableLiveData<LatLng> myLocation = new MutableLiveData<>();
}
