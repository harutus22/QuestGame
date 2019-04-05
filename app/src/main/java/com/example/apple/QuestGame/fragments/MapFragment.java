package com.example.apple.QuestGame.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.arsy.maps_library.MapRadar;
import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.live_data.CoinsLiveDataProvider;
import com.example.apple.QuestGame.live_data.MyLocationLiveData;
import com.example.apple.QuestGame.live_data.PointsLiveData;
import com.example.apple.QuestGame.live_data.QuestLiveData;
import com.example.apple.QuestGame.models.Coin;
import com.example.apple.QuestGame.models.Quest;
import com.example.apple.QuestGame.my_clusters.ClusterInfoViewAdapter;
import com.example.apple.QuestGame.my_clusters.ClusterRenderer;
import com.example.apple.QuestGame.utils.BitmapResize;
import com.example.apple.QuestGame.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.LOCATION_SERVICE;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private boolean locationPermission = false;
    private ClusterManager<Coin> mClusterManager;
    private ClusterRenderer mClusterRenderer;
    private DatabaseReference mDatabase;
    private Map<String, Coin> coins = new HashMap<>();
    private FirebaseAuth mAuth;
    private MapRadar mapRadar;
    private TextView points;
    private LocationManager mLocationManager;
    private Activity activity;
    private Context mContext;
    private PointsLiveData model;
    private ArrayList<Quest> quests;
    private Quest quest;
    private Marker questMarker;
    private boolean check;

    public MapFragment() { }

    public static MapFragment newInstance(String param) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(Constants.POINTS, param);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = ViewModelProviders.of(getActivity()).get(PointsLiveData.class);
        quests = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity = getActivity();
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        getLocationPermission();
        if(locationPermission) {
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            initPoints(view);
        }

    }

    @Override
    public void onAttach(Context context) {
        mLocationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        mContext = context;
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void initPoints(View group) {
        points = group.findViewById(R.id.mapFragmentUserPoints);
        model.getSelected().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                points.setText(s);
            }
        });

    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            CoinsLiveDataProvider.mCoins.observe(this, new Observer<HashMap<String, Coin>>() {
                @Override
                public void onChanged(@Nullable HashMap<String, Coin> coin) {
                    coins = coin;
                }
            });
            mapFragment.getMapAsync(this);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getQuests();
        setMapStyle(mMap);
        setUpCluster();
        zoomToMyLocation();
        getFusedLocation();
//        onClusterClick();
        initQuests();
        onMarkerClick();
        passQuest();
    }

    private void initQuests() {
        QuestLiveData.selected.observe(this, new Observer<Quest>() {
            @Override
            public void onChanged(@Nullable Quest quest) {
                quests.add(quest);
                String photoPath = Environment.getExternalStorageDirectory() + "/" + quest.getAvatar() + ".png";
                Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
                questMarker = mMap.addMarker(new MarkerOptions().position(quest.getCoordinate().get(0)).icon
                        (BitmapDescriptorFactory.fromBitmap(BitmapResize.getResizedBitmap(bitmap))).
                        title("quest").snippet(String.valueOf(quests.indexOf(quest))));
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void setMapStyle(GoogleMap mMap) {
        mMap.setMyLocationEnabled(true);
        int morning = 5;
        int afternoon = 13;
        int night = 21;
        int currentTime = getHour();

        if(currentTime >= morning && currentTime < afternoon){
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(mContext, R.raw.map_style_standart));
        }
        else if(currentTime >= afternoon && currentTime < night){
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(mContext, R.raw.map_style_retro));
        }
        else if (currentTime >= night && currentTime < 24 || currentTime < morning){
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(mContext, R.raw.map_style_night));
        }
    }

    private int getHour(){
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    @SuppressLint("MissingPermission")
    private void getFusedLocation() {
        MyLocationLiveData.myLocation.observe(this, new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location location) {
                addItems(location);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void setItemsToLocation(final Coin coin, Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mapRadar.withLatLng(latLng);
        if (SphericalUtil.computeDistanceBetween(latLng, coin.getPosition()) < 100 && !coin.isCluster()) {
            mClusterManager.addItem(coin);
            mClusterManager.cluster();
            Objects.requireNonNull(coins.get(coin.getTitle())).setCluster(true);
        } else if (SphericalUtil.computeDistanceBetween(latLng, coin.getPosition()) > 100 && coin.isCluster()) {
            mClusterManager.removeItem(coin);
            mClusterManager.cluster();
            try {
                Objects.requireNonNull(coins.get(coin.getTitle())).setCluster(false);
            } catch (NullPointerException o) {
                Log.d("cluster remov", o.getMessage());
            }
        }
    }

    @SuppressLint("MissingPermission")
    private Location getLastLocation() {
        Criteria criteria = new Criteria();
        String provider = mLocationManager.getBestProvider(criteria, true);
        return mLocationManager.getLastKnownLocation(provider);
    }

    private void initRadar() {
        //withRadarColors() have two parameters, startColor and tailColor respectively
        //startColor should start with transparency, here 00 in front of fccd29 indicates fully transparent
        //tailColor should end with opaqueness, here f in front of fccd29 indicates fully opaque

        Location location = getLastLocation();
        if (mapRadar == null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mapRadar = new MapRadar(mMap, latLng, mContext);
        }
        mapRadar.withDistance(100);
        mapRadar.withOuterCircleStrokeColor(0xfccd29);
        mapRadar.withRadarColors(0x00fccd29, 0xfffccd29);
        mapRadar.startRadarAnimation();
    }

    private void zoomToMyLocation() {
        Location location = getLastLocation();
        initRadar();

        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng myPosition = new LatLng(latitude, longitude);

            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(myPosition, 19);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(myPosition));
            mMap.animateCamera(yourLocation);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (locationPermission && checkMapServices()) {
            if (coins.isEmpty()) {
                addItems(getLastLocation());
            }
            if (mMap == null) {
                initMap();
            } else {
                initRadar();
                getQuests();
            }
        } else {
            getLocationPermission();
        }
    }

    private void goToMain() {
        FragmentTransaction fragmentManager = Objects.requireNonNull(getFragmentManager()).beginTransaction();
        fragmentManager.replace(R.id.placeHolder, new MainFragment());
        fragmentManager.commit();
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationPermission = true;
        }
    }

    public boolean isService() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext());

        if (available == ConnectionResult.SUCCESS) {
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(activity,
                    available, Constants.ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(mContext, "You can not make map requests", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public boolean isMapEnabled() {
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            noGpsMessage();
            return false;
        }
        return true;
    }

    private void noGpsMessage() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage("This app requires GPS turned on, do you want to enable it?")
                .setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent enableGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(enableGPS, Constants.ENABLE_GPS_CODE);
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                goToMain();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.ENABLE_GPS_CODE: {
                if (locationPermission) {
                    getQuests();
                } else {
                    getLocationPermission();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermission = false;
        switch (requestCode) {
            case Constants.LOCATION_REQUEST_CODE: {
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermission = true;
                }
            }
        }
    }

    private boolean checkMapServices() {
        if (isService()) {
            return isMapEnabled();
        }
        return false;
    }

    private void getQuests() {
        if(!quests.isEmpty()){

        }
    }


    private void setUpCluster() {
        if (mClusterManager == null) {
            mClusterManager = new ClusterManager<>(mContext, mMap);
        }

        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        if (mClusterRenderer == null) {
            mClusterRenderer = new ClusterRenderer(mContext, mMap, mClusterManager);
            mClusterManager.setRenderer(mClusterRenderer);
            mClusterManager.setAnimation(true);
        }
    }


    private void addItems(Location location) {
        for (Map.Entry<String, Coin> stringCoinEntry : coins.entrySet()) {
            setItemsToLocation(stringCoinEntry.getValue(), location);
        }
    }

    private void questBounds() {
//        limit of area that you can move camera
        final LatLngBounds ADELAIDE = new LatLngBounds(
                new LatLng(-35.0, 138.58), new LatLng(-34.9, 138.61));
        mMap.setLatLngBoundsForCameraTarget(ADELAIDE);
    }

    private void onClusterClick(final Marker marker) {
        points.setText(String.valueOf(Integer.valueOf(points.getText().toString()) +
                Integer.valueOf(marker.getSnippet())));
        final Coin clusterItem = coins.get(marker.getTitle());

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!marker.getTitle().equals("quest")) {
                    int point = Integer.valueOf(points.getText().toString());
                    mDatabase.child("users").child(mAuth.getUid()).child("points").setValue(point);
                    coins.remove(clusterItem.getTitle());
                    mClusterManager.removeItem(clusterItem);
                    mClusterManager.cluster();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setInfoWindow() {
        // when cluster clicked opens window with short description
        mClusterManager.getMarkerCollection()
                .setOnInfoWindowAdapter(new ClusterInfoViewAdapter(LayoutInflater.from(mContext)));

        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());
        onClusterWindowClick();
    }

    private void onClusterWindowClick() {
        // if quest window clicked open more informative window
        mClusterManager.setOnClusterItemInfoWindowClickListener(
                new ClusterManager.OnClusterItemInfoWindowClickListener<Coin>() {
                    @Override
                    public void onClusterItemInfoWindowClick(Coin stringClusterItem) {
                        Toast.makeText(mContext, "Clicked info window: " + stringClusterItem.getTitle(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        mMap.setOnInfoWindowClickListener(mClusterManager);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapRadar != null && mapRadar.isAnimationRunning()) {
            mapRadar.stopRadarAnimation();
        }
        for (Map.Entry<String, Coin> stringCoinEntry : coins.entrySet()) {
            stringCoinEntry.getValue().setCluster(false);
        }
        String message = points.getText().toString();
        model.select(message);
    }

    public void onMarkerClick() {
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(!check) {
                    if(marker.getTitle().equals("quest")) {
                        quest = quests.get(Integer.valueOf(marker.getSnippet()));

                        if (!quest.isAccepted()) {
                            quest.setOnButtonClick(onButtonClick);
                            quest.startQuest(getFragmentManager(), mContext, getActivity());
                        }
                    }
                }else {
                    passQuest();
                }
                onClusterClick(marker);
                return false;
            }
        });
    }

    private void passQuest(){
        if(quest != null){
            quest.passQuest(mMap, getFragmentManager(), this);
            if(quest.isFinished()){
                String point = String.valueOf(Integer.valueOf(points.getText().toString()) +
                        quest.getReward());
                model.select(point);
                points.setText(point);
                mDatabase.child("users").child(mAuth.getUid()).child("points").setValue(point);
                quests.remove(quest);
//                TODO after completing quest user don't get points, FIX IT
            }
        }
    }

    private Quest.OnButtonClick onButtonClick = new Quest.OnButtonClick() {
        @Override
        public void onClick(View view) {
            quest.setAccepted(true);
            quest.setZero(true);
            check = true;
            quest.setCount(quest.getCount() + 1);
            questMarker.remove();
            passQuest();
        }
    };

}


