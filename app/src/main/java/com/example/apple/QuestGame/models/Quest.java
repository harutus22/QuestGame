package com.example.apple.QuestGame.models;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class Quest {

    private String questId;
    private String name;
    private String description;
    private String avatar;
    private ArrayList<LatLng> coordinate;
    private ArrayList<String> questions;
    private int reward;

    public Quest(){
    }

    public Quest(String questId, String name, String description, String avatar,
                 ArrayList<LatLng> coordinate, ArrayList<String> questions, int reward) {
        this.questId = questId;
        this.name = name;
        this.description = description;
        this.avatar = avatar;
        this.coordinate = coordinate;
        this.questions = questions;
        this.reward = reward;
    }

    public String getQuestId() {
        return questId;
    }

    public void setQuestId(String questId) {
        this.questId = questId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public ArrayList<LatLng> getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(ArrayList<LatLng> coordinate) {
        this.coordinate = coordinate;
    }

    public ArrayList<String> getQuestions() {
        return questions;
    }

    public void setQuestions(ArrayList<String> questions) {
        this.questions = questions;
    }

    public int getReward() {
        return reward;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }
}
