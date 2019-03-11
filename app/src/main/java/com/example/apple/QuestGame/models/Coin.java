package com.example.apple.QuestGame.models;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class Coin implements ClusterItem{
    private LatLng position;
    private String title;
    private String snippet;
    private int iconPicture;


    public Coin(double lat, double lng, String title, String snippet, int iconPicture) {
        this.position = new LatLng(lat, lng);
        this.title = title;
        this.snippet = snippet;
        this.iconPicture = iconPicture;
    }

    public Coin(double lat, double lng) {
        position = new LatLng(lat, lng);
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }

    public int getIconPicture() {
        return iconPicture;
    }

    public void setIconPicture(int iconPicture) {
        this.iconPicture = iconPicture;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}
