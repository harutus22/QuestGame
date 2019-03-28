package com.example.apple.QuestGame.activities;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.live_data.CoinsLiveDataProvider;
import com.example.apple.QuestGame.live_data.MyLocationLiveData;
import com.example.apple.QuestGame.models.Coin;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.SphericalUtil;
import com.wikitude.architect.ArchitectStartupConfiguration;
import com.wikitude.architect.ArchitectView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class ArActivity extends AppCompatActivity {

    private static final int REQUEST_FINE_LOCATION = 200;
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    private ArchitectView mArchitectView;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Map<String, Coin> coinsData = new HashMap<>();
    private LatLng myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        checkPermissions();
        addArchitectView();
        getLocation();
        final JSONArray jsonArray = generateCoinInformation();
        mArchitectView.callJavascript("World.createModelAtLocation(" + jsonArray.toString() + ")");

    }

    private void addArchitectView() {
        mArchitectView = findViewById(R.id.architectView);
        final ArchitectStartupConfiguration config = new ArchitectStartupConfiguration();
        config.setFeatures(ArchitectStartupConfiguration.Features.Geo);
        config.setLicenseKey(getString(R.string.wikitude_license_key));
        mArchitectView.onCreate(config);

        CoinsLiveDataProvider.mCoins.observe(this, new Observer<HashMap<String, Coin>>() {
            @Override
            public void onChanged(@Nullable HashMap<String, Coin> coin) {
                coinsData = coin;
                getLocation();

                final ArchitectStartupConfiguration config = new ArchitectStartupConfiguration();
                config.setFeatures(ArchitectStartupConfiguration.Features.Geo);
                config.setLicenseKey(getString(R.string.wikitude_license_key));
                mArchitectView.onCreate(config);
            }
        });

        MyLocationLiveData.myLocation.observe(this, new Observer<LatLng>() {
            @Override
            public void onChanged(@Nullable LatLng latLng) {
                myLocation = latLng;
                final JSONArray jsonArray = generateCoinInformation();
                mArchitectView.callJavascript("World.createModelAtLocation(" + jsonArray.toString() + ")");
            }
        });


    }

    private void getLocation() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(5 * 1000);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    mArchitectView.setLocation(location.getLatitude(), location.getLongitude(), location.getAccuracy());
                }
            }
        };

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);

    }


    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_CAMERA_REQUEST_CODE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case REQUEST_FINE_LOCATION:
                if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            mArchitectView.setLocation(location.getLatitude(), location.getLongitude(), location.getAccuracy());
                        }
                    });
                }
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mArchitectView.onPostCreate();
        try {
            mArchitectView.load("3dModelAtGeo/index.html");
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_loading_ar_experience), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mArchitectView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mArchitectView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mArchitectView.clearCache();
        mArchitectView.onDestroy();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

    }


        private JSONArray generateCoinInformation() {

            final JSONArray coins = new JSONArray();
            final String ATTR_LATITUDE = "latitude";
            final String ATTR_LONGITUDE = "longitude";
            final HashMap<String, String> coinInformation = new HashMap<>();
            for (Map.Entry<String, Coin> stringCoinEntry : coinsData.entrySet()) {

                if (myLocation != null) {
                    LatLng latLng = myLocation;

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

        private void checkPermissions () {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_FINE_LOCATION);
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA},
                            MY_CAMERA_REQUEST_CODE);
                }
            }
        }


}
