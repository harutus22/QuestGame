package com.example.apple.QuestGame.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.models.ClusterInfoViewAdapter;
import com.example.apple.QuestGame.models.Coin;
import com.example.apple.QuestGame.utils.ClusterRenderer;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.GridBasedAlgorithm;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap mMap;
    private boolean mapType = false;
    private boolean locationPermission = false;
    private ClusterManager<Coin> mClusterManager;
    private ClusterRenderer mClusterRenderer;
    private LocationManager mLocationManager;
    private LocationListener mListener;
    private DatabaseReference mDatabase;
    private Map<String, Coin> coins;
    private FirebaseAuth mAuth;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        getLocationPermission();
        mAuth = FirebaseAuth.getInstance();
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        initMap();
    }

    private void initilizeMap() {
        coins = new HashMap<>();
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.child("coins").getChildren()) {

                    String key = dataSnapshot1.getKey();
                    String avatar = String.valueOf(dataSnapshot1.child("iconPicture").getValue());
                    String snipet = (String) dataSnapshot1.child("snippet").getValue();
                    String latitude = String.valueOf(dataSnapshot1.child("position").child("latitude").getValue());
                    String longitude = String.valueOf(dataSnapshot1.child("position").child("longitude").getValue());
                    Double lat = Double.parseDouble(latitude);
                    Double lng = Double.parseDouble(longitude);
                    Coin coin = new Coin(lat, lng, key, snipet, R.drawable.coin);
                    Log.d("coin", coin.getTitle() + "\n" + coin.getPosition() + coin.getSnippet());
                    coins.put(key, coin);
                }
                addItems();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mapType) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        LatLng aca = new LatLng(40.198887912292537, 44.490739703178408);
        mMap.addMarker(new MarkerOptions().position(aca).title("Coin in ACA"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(aca));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

//        setItemsToLocation();
        setUpCluster();
        initilizeMap();
        onClusterClick();
        zoomToMyLocation();
//        addItems();
    }

    private void setItemsToLocation(final Coin coin) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (SphericalUtil.computeDistanceBetween(latLng, coin.getPosition()) < 100) {
                    mClusterManager.addItem(coin);
                    mClusterManager.cluster();
                    Log.d("location", "100m");
                } else {
                        mClusterManager.removeItem(coin);
                        mClusterManager.cluster();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        mLocationManager.requestLocationUpdates("gps", 10000, 5, mListener);
    }

    private void zoomToMyLocation() {
        Criteria criteria = new Criteria();
        String provider = mLocationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = mLocationManager.getLastKnownLocation(provider);


        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng myPosition = new LatLng(latitude, longitude);


            LatLng coordinate = new LatLng(latitude, longitude);
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 19);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(myPosition));
            mMap.animateCamera(yourLocation);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getQuests();
        if (checkMapServices()) {
            if (locationPermission) {
                getQuests();
            } else {
                getLocationPermission();
            }
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
            if (isMapEnabled()) {
                return true;
            }
        }
        return false;
    }

    private void getQuests() {
    }

    // Declare a variable for the cluster manager.

    private void setUpCluster() {
        // Position the map.
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.198887912292537, 44.490739703178408), 10));

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        if (mClusterManager == null) {
            mClusterManager = new ClusterManager<>(this, mMap);
            mClusterManager.setAlgorithm((new PreCachingAlgorithmDecorator<>(new GridBasedAlgorithm<Coin>())));
        }
        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        if (mClusterRenderer == null) {
            mClusterRenderer = new ClusterRenderer(getApplicationContext(), mMap, mClusterManager);
            mClusterManager.setRenderer(mClusterRenderer);
        }
//        mMap.addMarker(markerOptions);
        mMap.setOnCameraIdleListener(mClusterManager);

        // Add cluster items (markers) to the cluster manager.
//        addItems();

    }


    private void addItems() {
        for (Map.Entry<String, Coin> stringCoinEntry : coins.entrySet()) {
            setItemsToLocation(stringCoinEntry.getValue());
        }

        // Set some lat/lng coordinates to start with.
//        double lat = 40.198887912292537;
//        double lng = 44.490739703178408;
//        int avatar = R.drawable.coin;
////        DefaultClusterRenderer
//
//        // Add ten cluster items in close proximity, for purposes of this example.
//        for (int i = 0; i < 100; i++) {
//            Coin offsetItem = new Coin(lat, lng, UUID.randomUUID().toString(), "10", avatar);
//            double offset = i / 60d;
//            lat = lat + offset;
//            lng = lng + offset;
//            setItemsToLocation(offsetItem);
//            mDatabase.child("coins").child(UUID.randomUUID().toString()).setValue(offsetItem);
//        }
//        Coin coin = new Coin(40.201499, 44.496076, "kangar", "", avatar);
//        setItemsToLocation(coin);
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

                        Toast.makeText(MapsActivity.this, "Cluster item click", Toast.LENGTH_SHORT).show();
                        mClusterManager.removeItem(clusterItem);
//                        coins.remove(clusterItem.getTitle());
                        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                long points = (long) dataSnapshot.child("users").child(mAuth.getUid()).child("points").getValue() +
                                        Long.valueOf(clusterItem.getSnippet());
                                mDatabase.child("users").child(mAuth.getUid()).child("points").setValue(points);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
//                        setInfoWindow();
                        mClusterManager.cluster();
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

}

