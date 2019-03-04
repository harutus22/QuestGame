package com.example.apple.QuestGame.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.models.Marker;
import com.example.apple.QuestGame.utils.ClusterRenderer;
import com.example.apple.QuestGame.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.GridBasedAlgorithm;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap mMap;
    private boolean mapType = false;
    private boolean locationPermission = false;
    private ClusterManager<Marker> mClusterManager;
    private MarkerOptions markerOptions;
    private ClusterRenderer mClusterRenderer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        getLocationPermission();
        initMap();
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
        if(mapType){
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        LatLng aca = new LatLng(40.198887912292537, 44.490739703178408);
        mMap.addMarker(new MarkerOptions().position(aca).title("Marker in ACA"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(aca));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
        setMyLocation();
        setUpCluster();
        onClusterClick();
    }

    private void setMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getQuests();
        if(checkMapServices()){
            if(locationPermission){
                getQuests();
            } else {
                getLocationPermission();
            }
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Constants.FINE_LOCATION) ==
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

    private boolean checkMapServices(){
        if(isService()){
            if(isMapEnabled()){
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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.198887912292537, 44.490739703178408), 10));

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<>(this, mMap);
        mClusterManager.setAlgorithm((new PreCachingAlgorithmDecorator<Marker>(new GridBasedAlgorithm<Marker>())));
        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        // Add cluster items (markers) to the cluster manager.
        addItems();
    }



    private void addItems() {

        // Set some lat/lng coordinates to start with.
        double lat = 40.198887912292537;
        double lng = 44.490739703178408;

//        DefaultClusterRenderer

        mClusterRenderer = new ClusterRenderer(getApplicationContext(), mMap, mClusterManager);
        mClusterManager.setRenderer(mClusterRenderer);

        // Add ten cluster items in close proximity, for purposes of this example.
        for (int i = 0; i < 100; i++) {
            double offset = i / 60d;
            lat = lat + offset;
            lng = lng + offset;
            int avatar = R.drawable.coin;
            Marker offsetItem = new Marker(lat, lng, "title" + lat, "snippet" + lng, avatar);

            mClusterManager.addItem(offsetItem);
        }
        mClusterManager.cluster();

    }

    private void questBounds(){
//        limit of area that you can move camera
        final LatLngBounds ADELAIDE = new LatLngBounds(
                new LatLng(-35.0, 138.58), new LatLng(-34.9, 138.61));

// Constrain the camera target to the Adelaide bounds.
        mMap.setLatLngBoundsForCameraTarget(ADELAIDE);
    }

    private void showMarkersNearby(){
        // show markers no near than 100 metres
        com.google.android.gms.maps.model.Marker locationMarker;
        markerOptions.visible(false);
        // We dont need to show, if its less than 100 meter we can show, otherwise we will just create and we will make it visble or not later
        locationMarker = mMap.addMarker(markerOptions);

        LatLng yourLatLang = new LatLng(40.198887912292537, 44.490739703178408);
        double lat = 40.198887912292537;
        double lng = 44.490739703178408;

        for (int i = 0; i < 10; i++) {
            double offset = i / 60d;
            lat = lat + offset;
            lng = lng + offset;
            Marker offsetItem = new Marker(lat, lng);
            mClusterManager.addItem(offsetItem);

        }
        if (SphericalUtil.computeDistanceBetween(yourLatLang, locationMarker.getPosition()) < 100) {
            locationMarker.setVisible(true);
        }
    }

    private void onClusterClick(){
        mClusterManager.setOnClusterItemClickListener(
                new ClusterManager.OnClusterItemClickListener<Marker>() {
                    @Override public boolean onClusterItemClick(Marker clusterItem) {

                        Toast.makeText(MapsActivity.this, "Cluster item click", Toast.LENGTH_SHORT).show();
                        mClusterManager.removeItem(clusterItem);
                        mClusterManager.cluster();
                        // if true, click handling stops here and do not show info view, do not move camera
                        // you can avoid this by calling:
                        // renderer.getMarker(clusterItem).showInfoWindow();

                        return false;
                    }
                });
    }


}

