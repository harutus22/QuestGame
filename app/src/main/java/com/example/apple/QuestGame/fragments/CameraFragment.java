package com.example.apple.QuestGame.fragments;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
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
import com.wikitude.architect.ArchitectStartupConfiguration;
import com.wikitude.architect.ArchitectView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CameraFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final int REQUEST_FINE_LOCATION = 200;
    ArchitectView mArchitectView;

    private Map<String, Coin> coinsData = new HashMap<>();
    private Location myLocation;

    private static final int MY_CAMERA_REQUEST_CODE = 100;

    public CameraFragment() { }

    public static CameraFragment newInstance(String param1, String param2) {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        getCoins();
        getLocation();
    }

    private void getLocation() {
        MyLocationLiveData.myLocation.observe(this, new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location location) {
                myLocation = location;
                final JSONArray jsonArray = generatePoiInformation();
                mArchitectView.setLocation(location.getLatitude(), location.getLongitude(), location.getAccuracy());
                mArchitectView.callJavascript("World.createModelAtLocation(" + jsonArray.toString() + ")");
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


    @SuppressLint("MissingPermission")
    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case MY_CAMERA_REQUEST_CODE:
            {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getActivity(), "camera permission granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), "camera permission denied", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case REQUEST_FINE_LOCATION:
                if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();
                }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ArchitectStartupConfiguration config = new ArchitectStartupConfiguration();
        config.setFeatures(ArchitectStartupConfiguration.Features.Geo);
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
        mArchitectView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mArchitectView.clearCache();
        mArchitectView.onDestroy();

    }

    private JSONArray generatePoiInformation() {

        final JSONArray coins = new JSONArray();

        final String ATTR_LATITUDE = "latitude";
        final String ATTR_LONGITUDE = "longitude";

        final HashMap<String, String> coinInformation = new HashMap<>();

        for (Map.Entry<String, Coin> stringCoinEntry : coinsData.entrySet()) {

            if(myLocation != null) {
                LatLng latLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());

                double[] coinLocationLatLon = new double[]{stringCoinEntry.getValue().getPosition().latitude, stringCoinEntry.getValue().getPosition().longitude};

                if (SphericalUtil.computeDistanceBetween(latLng, stringCoinEntry.getValue().getPosition()) < 15 &&
                        !stringCoinEntry.getValue().isCluster()) {
                    coinInformation.put(ATTR_LATITUDE, String.valueOf(coinLocationLatLon[0]));
                    coinInformation.put(ATTR_LONGITUDE, String.valueOf(coinLocationLatLon[1]));
                    coins.put(new JSONObject(coinInformation));
                }
            }
        }

        return coins;
    }
}
