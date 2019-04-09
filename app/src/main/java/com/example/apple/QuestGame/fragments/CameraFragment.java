package com.example.apple.QuestGame.fragments;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.live_data.CoinsLiveDataProvider;
import com.example.apple.QuestGame.live_data.MyLocationLiveData;
import com.example.apple.QuestGame.models.Coin;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.wikitude.architect.ArchitectJavaScriptInterfaceListener;
import com.wikitude.architect.ArchitectStartupConfiguration;
import com.wikitude.architect.ArchitectView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CameraFragment extends Fragment implements ArchitectJavaScriptInterfaceListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private boolean canCreateModel;
    private boolean isInstantTracking;

    ArchitectView mArchitectView;

    private HashMap<String, Coin> coinsData = new HashMap<>();
    private Location myLocation;

    public CameraFragment() { }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        canCreateModel = true;
        isInstantTracking = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mArchitectView = view.findViewById(R.id.architectView);
        mArchitectView.addArchitectJavaScriptInterfaceListener(CameraFragment.this);
        getCoins();
        getLocation();

    }

    private void getLocation() {
        MyLocationLiveData.myLocation.observe(this, new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location location) {
                myLocation = location;
                mArchitectView.setLocation(location.getLatitude(), location.getLongitude(), location.getAccuracy());
                if(myLocation != null && canCreateModel) {
                    canCreateModel = false;
                    createModelAtLocation();
                }

            }
        });
    }

    private void getCoins() {
        CoinsLiveDataProvider.mCoins.observe(this, new Observer<HashMap<String, Coin>>() {
            @Override
            public void onChanged(@Nullable HashMap<String, Coin> coin) {
                coinsData = coin;
            }
        });
    }




    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        final ArchitectStartupConfiguration config = new ArchitectStartupConfiguration();
        config.setFeatures(ArchitectStartupConfiguration.Features.Geo);
        config.setFeatures(ArchitectStartupConfiguration.Features.InstantTracking);
        config.setLicenseKey(getString(R.string.wikitude_license_key));
        try {
            mArchitectView.onCreate(config);
            mArchitectView.onPostCreate();
        } catch (RuntimeException rex) {
            this.mArchitectView = null;
            Toast.makeText(getActivity().getApplicationContext(), "can't create Architect View", Toast.LENGTH_SHORT).show();
            Log.e(this.getClass().getName(), "Exception in ArchitectView.onCreate()", rex);
        }


        try {
            mArchitectView.load("3dModelAtGeo/index.html");
        } catch (IOException e) {
            Toast.makeText(getActivity(), getString(R.string.error_loading_ar_experience), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mArchitectView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mArchitectView.clearCache();
        mArchitectView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mArchitectView.clearCache();
        mArchitectView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mArchitectView.onLowMemory();
    }

    private void createModelAtLocation(){
            Map.Entry<String, Coin> entry = null;

        for (Map.Entry<String, Coin> stringCoinEntry : coinsData.entrySet()) {

                LatLng latLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                if (SphericalUtil.computeDistanceBetween(latLng, stringCoinEntry.getValue().getPosition()) < 50 ) {
                    entry = stringCoinEntry;
                   final double[] coinLocationLatLon = new double[]{entry.getValue().getPosition().latitude, entry.getValue().getPosition().longitude};
                   final String key = entry.getKey();
                    if (SphericalUtil.computeDistanceBetween(latLng, stringCoinEntry.getValue().getPosition()) < 25 ) {
                        if(!isInstantTracking){
                            final JSONArray jsonArray = generateCoinInformation(coinLocationLatLon, key);
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    changeTrackerState();
                                    mArchitectView.callJavascript("World.addModel(" + jsonArray.toString() + ")");
                                }
                            }, 1500);
                        }

                    }else {
                        if(isInstantTracking){
                            changeTrackerState();
                        }
                        final JSONArray jsonArray = generateCoinInformation(coinLocationLatLon, key);
                        mArchitectView.callJavascript("World.addModel(" + jsonArray.toString() + ")");

                    }
                }
           }
        if(entry == null){
            canCreateModel = true;
        }
    }


    private JSONArray generateCoinInformation(double[] coinLocationLatLon, String key) {

        final JSONArray coins = new JSONArray();

        final String ATTR_KEY = "key";
        final String ATTR_LATITUDE = "latitude";
        final String ATTR_LONGITUDE = "longitude";

        final HashMap<String, String> coinInformation = new HashMap<>();

        coinInformation.put(ATTR_KEY, key);
        coinInformation.put(ATTR_LATITUDE, String.valueOf(coinLocationLatLon[0]));
        coinInformation.put(ATTR_LONGITUDE, String.valueOf(coinLocationLatLon[1]));
        coins.put(new JSONObject(coinInformation));

        return coins;
    }

    private void changeTrackerState(){
        mArchitectView.callJavascript("World.changeTrackerState()");
        isInstantTracking = !isInstantTracking;
    }

    @Override
    public void onJSONObjectReceived(JSONObject jsonObject) {
        try {
            coinsData.remove(jsonObject.getString("key"));
            CoinsLiveDataProvider.mCoins.postValue(coinsData);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}
