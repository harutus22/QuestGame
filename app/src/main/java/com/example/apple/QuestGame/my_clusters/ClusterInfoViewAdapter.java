package com.example.apple.QuestGame.my_clusters;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.apple.QuestGame.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class ClusterInfoViewAdapter implements GoogleMap.InfoWindowAdapter {
    private LayoutInflater mLayoutInflater;

    public ClusterInfoViewAdapter(LayoutInflater layoutInflater){
         mLayoutInflater = layoutInflater;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return inflateLayout(marker);
    }

    @Override
    public View getInfoContents(Marker marker) {
        return inflateLayout(marker);
    }

    private View inflateLayout(Marker marker){
        final View popup = mLayoutInflater.inflate(R.layout.info_window_layout, null);
        ((TextView) popup.findViewById(R.id.title)).setText(marker.getSnippet());
        return popup;
    }
}
