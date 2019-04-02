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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
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
import com.example.apple.QuestGame.models.Coin;
import com.example.apple.QuestGame.my_clusters.ClusterInfoViewAdapter;
import com.example.apple.QuestGame.my_clusters.ClusterRenderer;
import com.example.apple.QuestGame.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.ClusterManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

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
    private Context context;
    private PointsLiveData model;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity = getActivity();
        context = getContext();
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
            mapFragment.getMapAsync(this);
            CoinsLiveDataProvider.mCoins.observe(this, new Observer<HashMap<String, Coin>>() {
                @Override
                public void onChanged(@Nullable HashMap<String, Coin> coin) {
                    coins = coin;
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        setMapStyle(mMap);

        setUpCluster();
        zoomToMyLocation();
        getFusedLocation();
        onClusterClick();
    }

    @SuppressLint("MissingPermission")
    private void setMapStyle(GoogleMap mMap) {
        mMap.setMyLocationEnabled(true);
        int morning = 5;
        int afternoon = 13;
        int night = 21;
        int currentTime = getHour();

        Log.d("time", String.valueOf(getHour()));

        if(currentTime >= morning && currentTime < afternoon){
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_standart));
        }
        else if(currentTime >= afternoon && currentTime < night){
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_retro));
        }
        else if (currentTime >= night && currentTime < 24 || currentTime < morning){
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_night));
        }
    }

    private int getHour(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        String formattedDate = df.format(calendar.getTime());
        String string = TextUtils.substring(formattedDate, 0, 4);
        StringTokenizer tokenizer = new StringTokenizer(string, ":");
        String time = tokenizer.nextToken();
        return Integer.valueOf(time);
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
            Log.d("location", "100m");
        } else if (SphericalUtil.computeDistanceBetween(latLng, coin.getPosition()) > 100 && coin.isCluster()) {
            mClusterManager.removeItem(coin);
            mClusterManager.cluster();
            try {
                Objects.requireNonNull(coins.get(coin.getTitle())).setCluster(false);
            } catch (NullPointerException o) {
                Log.d("true-false", "catched");
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
            mapRadar = new MapRadar(mMap, latLng, getContext());
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
            getQuests();
            if (coins.isEmpty()) {
                addItems(getLastLocation());
            }
            if (mMap == null) {
                initMap();
            } else {
                initRadar();
            }
        } else {
            getLocationPermission();
        }
    }

    private void goToMain() {
        FragmentTransaction fragmentManager = getFragmentManager().beginTransaction();
        fragmentManager.replace(R.id.placeHolder, new MainFragment());
        fragmentManager.commit();
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(context.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationPermission = true;
            getQuests();
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Constants.FINE_LOCATION},
                    Constants.LOCATION_REQUEST_CODE);
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
            Toast.makeText(context, "You can not make map requests", Toast.LENGTH_LONG).show();
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
    }


    private void setUpCluster() {
        if (mClusterManager == null) {
            mClusterManager = new ClusterManager<>(context, mMap);
        }

        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        if (mClusterRenderer == null) {
            mClusterRenderer = new ClusterRenderer(context, mMap, mClusterManager);
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

// Constrain the camera target to the Adelaide bounds.
        mMap.setLatLngBoundsForCameraTarget(ADELAIDE);
    }

    private void onClusterClick() {
        mClusterManager.setOnClusterItemClickListener(
                new ClusterManager.OnClusterItemClickListener<Coin>() {
                    @Override
                    public boolean onClusterItemClick(final Coin clusterItem) {

                        points.setText(String.valueOf(Integer.valueOf(points.getText().toString()) +
                                Integer.valueOf(clusterItem.getSnippet())));

                        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                int point = Integer.valueOf(points.getText().toString());
                                mDatabase.child("users").child(mAuth.getUid()).child("points").setValue(point);
                                coins.remove(clusterItem.getTitle());
                                mClusterManager.removeItem(clusterItem);
                                mClusterManager.cluster();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
//                        setInfoWindow();

                        // if true, click handling stops here and do not show info view, do not move camera
                        // you can avoid this by calling:
                        // renderer.getMarker(clusterItem).showInfoWindow();

                        return false;
                    }
                });
    }

    private void setInfoWindow() {
        // when cluster clicked opens window with short description
        mClusterManager.getMarkerCollection()
                .setOnInfoWindowAdapter(new ClusterInfoViewAdapter(LayoutInflater.from(context)));

        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());
        onClusterWindowClick();
    }

    private void onClusterWindowClick() {
        // if quest window clicked open more informative window
        mClusterManager.setOnClusterItemInfoWindowClickListener(
                new ClusterManager.OnClusterItemInfoWindowClickListener<Coin>() {
                    @Override
                    public void onClusterItemInfoWindowClick(Coin stringClusterItem) {
                        Toast.makeText(context, "Clicked info window: " + stringClusterItem.getTitle(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        mMap.setOnInfoWindowClickListener(mClusterManager);
    }

    @Override
    public void onStop() {
        super.onStop();
        if ( mapRadar != null && mapRadar.isAnimationRunning()) {
            mapRadar.stopRadarAnimation();
        }
        for (Map.Entry<String, Coin> stringCoinEntry : coins.entrySet()) {
            stringCoinEntry.getValue().setCluster(false);
        }
        String message = points.getText().toString();
        model.select(message);
    }


}


