package com.example.apple.QuestGame.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import com.arsy.maps_library.MapRadar;
import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.live_data.CoinsLiveDataProvider;
import com.example.apple.QuestGame.live_data.MyLocationLiveData;
import com.example.apple.QuestGame.my_clusters.ClusterInfoViewAdapter;
import com.example.apple.QuestGame.models.Coin;
import com.example.apple.QuestGame.my_clusters.ClusterRenderer;
import com.example.apple.QuestGame.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.ClusterManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap mMap;
    private boolean locationPermission = false;
    private ClusterManager<Coin> mClusterManager;
    private ClusterRenderer mClusterRenderer;
    private LocationManager mLocationManager;
    private DatabaseReference mDatabase;
    private Map<String, Coin> coins = new HashMap<>();
    private FirebaseAuth mAuth;
    private MapRadar mapRadar;
    private TextView points;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        getLocationPermission();
        if(locationPermission) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mAuth = FirebaseAuth.getInstance();
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            mDatabase = FirebaseDatabase.getInstance().getReference();
            initPoints();
        }
    }

    private void initPoints() {
        points = findViewById(R.id.mapUserPoints);
        int getPoints = getIntent().getIntExtra(Constants.POINTS, 0);
        points.setText(String.valueOf(getPoints));
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
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
        mMap.setMyLocationEnabled(true);
        setUpCluster();
        zoomToMyLocation();
        getFusedLocation();
        onClusterClick();
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
            mapRadar = new MapRadar(mMap, latLng, getApplicationContext());
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
    protected void onResume() {
        super.onResume();
        if (coins.isEmpty()) {
            addItems(getLastLocation());
        }
        if (mMap == null) {
            if (locationPermission) {
                initMap();
            } else {
                finish();
            }
        } else {
            initRadar();
        }
        if (checkMapServices()) {
            getQuests();
        } else {
            getLocationPermission();
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationPermission = true;
            getQuests();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Constants.FINE_LOCATION},
                    Constants.LOCATION_REQUEST_CODE);
        }
    }

    public boolean isService() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        if (available == ConnectionResult.SUCCESS) {
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this,
                    available, Constants.ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can not make map requests", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public boolean isMapEnabled() {
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            noGpsMessage();
            return false;
        }
        return true;
    }

    private void noGpsMessage() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                finish();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
        // Position the map.

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        if (mClusterManager == null) {
            mClusterManager = new ClusterManager<>(this, mMap);
        }
        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        if (mClusterRenderer == null) {
            mClusterRenderer = new ClusterRenderer(getApplicationContext(), mMap, mClusterManager);
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
                .setOnInfoWindowAdapter(new ClusterInfoViewAdapter(LayoutInflater.from(this)));

        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());
        onClusterWindowClick();
    }

    private void onClusterWindowClick() {
        // if quest window clicked open more informative window
        mClusterManager.setOnClusterItemInfoWindowClickListener(
                new ClusterManager.OnClusterItemInfoWindowClickListener<Coin>() {
                    @Override
                    public void onClusterItemInfoWindowClick(Coin stringClusterItem) {
                        Toast.makeText(MapsActivity.this, "Clicked info window: " + stringClusterItem.getTitle(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        mMap.setOnInfoWindowClickListener(mClusterManager);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapRadar.isAnimationRunning() && mapRadar != null) {
            mapRadar.stopRadarAnimation();
        }
        for (Map.Entry<String, Coin> stringCoinEntry : coins.entrySet()) {
            stringCoinEntry.getValue().setCluster(false);
        }
    }
}

