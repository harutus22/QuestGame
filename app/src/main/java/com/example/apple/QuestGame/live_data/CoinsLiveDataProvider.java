package com.example.apple.QuestGame.live_data;

import android.arch.lifecycle.MutableLiveData;

import com.example.apple.QuestGame.models.Coin;

import java.util.HashMap;
import java.util.List;

public class CoinsLiveDataProvider {
    public static MutableLiveData<HashMap<String, Coin>> mCoins = new MutableLiveData<>();

}
