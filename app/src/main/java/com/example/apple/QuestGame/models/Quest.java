package com.example.apple.QuestGame.models;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.Button;

import com.example.apple.QuestGame.live_data.MyLocationLiveData;
import com.example.apple.QuestGame.utils.PopUpDialog;
import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.utils.BitmapResize;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

public class Quest {

    private String questId;
    private String name;
    private String description;
    private String avatar;
    private ArrayList<LatLng> coordinate;
    private ArrayList<String> questions;
    private int reward;
    private PopUpDialog popUpDialog;
    private Context mContext;
    private Bitmap icon;
    private OnButtonClick onButtonClick;
    private Marker marker;
    private int count = 0;
    private boolean accepted, zero, first, second, third, fourth, fifth, sixth, finished;

    public Quest() {
    }

    private PopUpDialog.OnButtonClick buttonClick = new PopUpDialog.OnButtonClick() {
        @Override
        public void onClick(View button) {
            if(onButtonClick != null){
                onButtonClick.onClick(button);
                button.setVisibility(View.GONE);
            }
        }
    };

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

    public void setOnButtonClick(OnButtonClick onButtonClick) {
        this.onButtonClick = onButtonClick;
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

    public int getReward() { return reward; }

    public void setReward(int reward) {
        this.reward = reward;
    }

    public void setZero(boolean zero) { this.zero = zero; }

    public boolean isFinished() { return finished; }

    public int getCount() { return count; }

    public void setCount(int count) { this.count = count; }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void startQuest(FragmentManager fragmentManager, Context context) {
        if(popUpDialog == null){
           popUpDialog = new PopUpDialog();
        }
        mContext = context;
        popUpDialog.setTitle(getName());
        popUpDialog.setDescription(getDescription());
        popUpDialog.show(fragmentManager,"pop");
        popUpDialog.setOnButtonClick(buttonClick);
    }

    public void passQuest(final GoogleMap mMap, final FragmentManager fragmentManager, LifecycleOwner lifecycle){
        if(accepted) {
            MyLocationLiveData.myLocation.observe(lifecycle, new Observer<Location>() {
                @Override
                public void onChanged(@Nullable Location location) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                        if (zero) {

                            icon = BitmapFactory.decodeResource(mContext.getResources(),
                                    R.drawable.question_marker);
                            popUpDialog.dismiss();
                            popUpDialog.setDescription(getQuestions().get(count + 1));
                            popUpDialog.show(fragmentManager, "pop");
                            zero = false;
                        }
                        if (count == 1) {
                            setMarker(mMap, count, fragmentManager, latLng);
                        }

//                        } else if (count == 2) {
////                            removeMarker();
//                            setMarker(mMap, 2, fragmentManager, latLng);
//                        } else if (count == 3) {
////                            removeMarker();
//                            setMarker(mMap, 3, fragmentManager, latLng);
//                        } else if (count == 4) {
////                            removeMarker();
//                            setMarker(mMap, 4, fragmentManager, latLng);
//                        } else if (count == 5) {
////                            removeMarker();
//                            setMarker(mMap, 5, fragmentManager, latLng);
//                        }
                        else if (count == 6) {
//                            removeMarker();
                            accepted = false;
                            finished = true;
                        } else {
                            setMarker(mMap, count, fragmentManager, latLng);
                    }
                }
            });

        }
    }

    private void setMarker(GoogleMap mMap, int number, final FragmentManager fragmentManager, LatLng latLng){
        if(marker == null) {
            popUpDialog.setDescription(getQuestions().get(number - 1));
            if(!popUpDialog.isRemoving()) {
//                popUpDialog.show(fragmentManager, "pop");
            }
            marker = mMap.addMarker(new MarkerOptions().
                    icon(BitmapDescriptorFactory.fromBitmap(BitmapResize.getResizedBitmap(icon))).
                    position(getCoordinate().get(number)));
            marker.setVisible(false);
        } else {
            if (checkDistance(latLng)) {
                if(!marker.isVisible()) {
                    marker.setVisible(true);
                }
                final int numb = number;
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        count++;
                        removeMarker();
                        popUpDialog.setDescription(getQuestions().get(numb));
                        popUpDialog.show(fragmentManager, "pop");
                        return false;
                    }
                });
            } else {
                if(marker.isVisible()) {
                    marker.setVisible(false);
                }
            }
        }
    }

    private boolean checkDistance(LatLng myLocation){
        int a = (int) SphericalUtil.computeDistanceBetween(myLocation, coordinate.get(count));
        return SphericalUtil.computeDistanceBetween(myLocation, coordinate.get(count)) < 15;
    }

    private void removeMarker(){
        marker.remove();
        marker = null;
    }

    public interface OnButtonClick{
        void onClick(View view);
    }
}
