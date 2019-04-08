package com.example.apple.QuestGame.services;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;

import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.live_data.CoinsLiveDataProvider;
import com.example.apple.QuestGame.live_data.MyLocationLiveData;
import com.example.apple.QuestGame.live_data.QuestLiveData;
import com.example.apple.QuestGame.models.Coin;
import com.example.apple.QuestGame.models.Quest;
import com.example.apple.QuestGame.utils.Constants;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class LocationService extends IntentService {

    private FusedLocationProviderClient mFusedLocationClient;

    private DatabaseReference mDatabase;
    private HashMap<String, Coin> coins = new HashMap<>();
    private final int avatar = R.drawable.coin;
    private StorageReference mStorageRef;
    private HashSet<Quest> quests;


    public LocationService() {
        super("MyLocation");
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference().child("quest");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        quests = new HashSet<>();

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "My Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("fafaa")
                    .setContentText("sdassdada").build();

            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getLocation();
        getQuests();
        return START_NOT_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {

        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(Constants.UPDATE_INTERVAL);
        mLocationRequestHighAccuracy.setFastestInterval(Constants.FASTEST_INTERVAL);
        mLocationRequestHighAccuracy.setSmallestDisplacement(Constants.DISPLACEMENT_UPDATE);
        getArrayList();
        mFusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Location location = locationResult.getLastLocation();
                        double latitude = locationResult.getLastLocation().getLatitude();
                        double longitude = locationResult.getLastLocation().getLongitude();

                        if (location != null) {
                            Log.d("move", String.valueOf(latitude) + " " +
                                    String.valueOf(longitude));
                            MyLocationLiveData.myLocation.setValue(location);
                        }
                    }
                },
                Looper.myLooper());
    }

    private void getArrayList() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.child("coins").getChildren()) {

                    String key = dataSnapshot1.getKey();
                    String snippet = dataSnapshot1.child("snippet").getValue(String.class);
                    Double latitude = dataSnapshot1.child("position").child("latitude").getValue(Double.class);
                    Double longitude = dataSnapshot1.child("position").child("longitude").getValue(Double.class);
                    Coin coin = new Coin(latitude, longitude, key, snippet, avatar);
                    Log.d("coin", coin.getTitle() + "\n" + coin.getPosition() + coin.getSnippet());

                    coins.put(key, coin);
                }
                CoinsLiveDataProvider.mCoins.setValue(coins);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getQuests() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<ArrayList<String>> ques = new GenericTypeIndicator<ArrayList<String>>() {};
                for (DataSnapshot dataSnapshot1 : dataSnapshot.child("quest").getChildren()){

                    String questId = dataSnapshot1.getKey();
                    String avatar = dataSnapshot1.child("avatar").getValue(String.class);
                        String description = dataSnapshot1.child("description").getValue(String.class);
                        String name = dataSnapshot1.child("name").getValue(String.class);
                        int reward = dataSnapshot1.child("reward").getValue(Integer.class);
                        ArrayList<String> questions = dataSnapshot1.child("questions").getValue(ques);
                        ArrayList<LatLng> coordinates = new ArrayList<>();
                        for (DataSnapshot coordinate : dataSnapshot1.child("locations").getChildren()) {
                            Double latitude = coordinate.child("latitude").getValue(Double.class);
                            Double longitude = coordinate.child("longitude").getValue(Double.class);
                            LatLng latLng = new LatLng(latitude, longitude);
                            coordinates.add(latLng);
                        }
                        Quest quest = new Quest(questId, name, description, avatar, coordinates, questions, reward);
                        QuestLiveData.selected.setValue(quest);
                    if(!checkImageAvailability(avatar)) {
                        saveImage(avatar);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void saveImage(final String string){
        StorageReference ref = mStorageRef.child(string + ".png");
        final long ONE_MEGABYTE = 1024 * 1024;
        ref.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                saveImageToStorage(bmp, string);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
            }
        });
    }

    private void saveImageToStorage(Bitmap bmp, String imageName)
    {
        File file = getFile(imageName);
        OutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        try {
            fOut.flush();
            fOut.close();
            MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getFile(String imageName){
        String path = Environment.getExternalStorageDirectory().toString();
        return new File(path, imageName + ".png");
    }

    private boolean checkImageAvailability(String imageNAme){
        File file = getFile(imageNAme);
        return file.exists();
    }
}
