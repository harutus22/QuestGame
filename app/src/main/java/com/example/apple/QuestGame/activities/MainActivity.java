package com.example.apple.QuestGame.activities;

import android.Manifest;
import android.app.ActivityManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.fragments.CameraFragment;
import com.example.apple.QuestGame.fragments.MainFragment;
import com.example.apple.QuestGame.fragments.MapFragment;
import com.example.apple.QuestGame.live_data.PointsLiveData;
import com.example.apple.QuestGame.models.User;
import com.example.apple.QuestGame.services.LocationService;
import com.example.apple.QuestGame.utils.Constants;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.OnDisconnect;
import com.google.firebase.database.ValueEventListener;



public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {


    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int REQUEST_FINE_LOCATION = 200;
    BottomNavigationView mBottomNavigationView;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean connected;
    private MainFragment mainFragment;
    private String userId;
    private DatabaseReference mRef;
    private String name;
    private String point;
    private PointsLiveData model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        model = ViewModelProviders.of(this).get(PointsLiveData.class);
        init();
        getLocationPermission();
        checkPermissions();
    }

    private void fireBaseInit() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }
        mRef = FirebaseDatabase.getInstance().getReference();
        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                name = dataSnapshot.child("users").child(userId).getValue(User.class).getUser_name();
                point = String.valueOf(dataSnapshot.child("users").child(userId).getValue(User.class).getPoints());
                FragmentTransaction fragmentManager = getSupportFragmentManager().beginTransaction();
                mainFragment = MainFragment.newInstance(name, point);
                fragmentManager.add(R.id.placeHolder, mainFragment);
                fragmentManager.commit();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    private void init(){
        mBottomNavigationView = findViewById(R.id.bottom_navigation_view);
        mBottomNavigationView.setOnNavigationItemSelectedListener(this);
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Constants.FINE_LOCATION},
                    Constants.LOCATION_REQUEST_CODE);
        } else {
            startLocationService();
            fireBaseInit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.LOCATION_REQUEST_CODE: {
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationService();
                    fireBaseInit();
                }
                else {
                    System.exit(0);
                    finish();
                }
                break;
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        FragmentTransaction fragmentManager = getSupportFragmentManager().beginTransaction();
        if(model.getSelected().getValue() != null){
            model.getSelected().observe(this, new Observer<String>() {
                @Override
                public void onChanged(@Nullable String s) {
                    point = s;
                }
            });
        }
        switch (menuItem.getItemId()){


            case R.id.action_map:
            {
                fragmentManager.replace(R.id.placeHolder, new MapFragment());
                fragmentManager.commit();
            } break;

            case R.id.action_quests:
            {
                MainFragment mainFragment = MainFragment.newInstance(name, point);
                fragmentManager.replace(R.id.placeHolder, mainFragment);
                fragmentManager.commit();
            } break;

            case R.id.action_play:
            {
//                Intent intent = new Intent(this, ArActivity.class);
//                startActivity(intent);
                fragmentManager.replace(R.id.placeHolder, new CameraFragment());
                fragmentManager.commit();
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String check = "";
        switch (item.getItemId()){
            case R.id.logOut:{
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    for (UserInfo profile : user.getProviderData()) {
                        check = profile.getProviderId();
                        Toast.makeText(this, check, Toast.LENGTH_LONG).show();
                    }
                }

                if(check.equals("facebook.com")) {
                    signOut();
                    LoginManager.getInstance().logOut();
                } else if(check.equals("google.com")){
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.google_id_client))
                            .requestEmail().requestId().requestProfile()
                            .build();

                    mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
                    mGoogleSignInClient.revokeAccess().addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            signOut();
                            LoginManager.getInstance().logOut();
                        }
                    });
                } else if (check.equals("password")){
                    signOut();
                }
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut(){
        FirebaseAuth.getInstance().signOut();
    }


    private void getUserInfo(){
        //TODO use to get user info
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        userId = user.getUid();
        String check = "";
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                check = profile.getProviderId();
                Toast.makeText(this, check, Toast.LENGTH_LONG).show();
            }
        }

//        } else if (check.equals("google.com")){
//            for (UserInfo profile : user.getProviderData()) {
//                // Id of the provider (ex: google.com)
//                String providerId = profile.getProviderId();
//
//                // UID specific to the provider
//                String uid = profile.getUid();
//
//                // Name, email address, and profile photo Url
//                String name = profile.getDisplayName();
//                String email = profile.getEmail();
//                Uri photoUrl = profile.getPhotoUrl();
//
//                mUsername.setText(uid);
//                mPoints.setText(String.valueOf("0"));
//            }
//        }
//
//        if (user != null) {
//            // Name, email address, and profile photo Url
//
//
//
//
//            // Check if user's email is verified
//            boolean emailVerified = user.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getIdToken() instead.
//            String uid = user.getUid();

//            //TODO update user
//
//            FirebaseUser user1 = FirebaseAuth.getInstance().getCurrentUser();
//
//            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
//                    .setDisplayName("Jane Q. User")
//                    .setPhotoUri(Uri.parse("https://example.com/jane-q-user/profile.jpg"))
//                    .build();
//
//            user.updateProfile(profileUpdates)
//                    .addOnCompleteListener(new OnCompleteListener<Void>() {
//                        @Override
//                        public void onComplete(@NonNull Task<Void> task) {
//                            if (task.isSuccessful()) {
//                                Log.d("what", "User profile updated.");
//                            }
//                        }
//                    });
//
//            //Email update
//
//            user.updateEmail("user@example.com")
//                    .addOnCompleteListener(new OnCompleteListener<Void>() {
//                        @Override
//                        public void onComplete(@NonNull Task<Void> task) {
//                            if (task.isSuccessful()) {
//                                Log.d("Email", "User email address updated.");
//                            }
//                        }
//                    });
//
//            // sent verification Email
//            FirebaseAuth auth = FirebaseAuth.getInstance();
//            FirebaseUser user3 = auth.getCurrentUser();
//
//            user3.sendEmailVerification()
//                    .addOnCompleteListener(new OnCompleteListener<Void>() {
//                        @Override
//                        public void onComplete(@NonNull Task<Void> task) {
//                            if (task.isSuccessful()) {
//                                Log.d("ga", "Email sent.");
//                            }
//                        }
//                    });
//
//            //set Password
//            FirebaseUser user4 = FirebaseAuth.getInstance().getCurrentUser();
//            String newPassword = "SOME-SECURE-PASSWORD";
//
//            user4.updatePassword(newPassword)
//                    .addOnCompleteListener(new OnCompleteListener<Void>() {
//                        @Override
//                        public void onComplete(@NonNull Task<Void> task) {
//                            if (task.isSuccessful()) {
//                                Log.d("password", "User password updated.");
//                            }
//                        }
//                    });
//        }
    }

    // Read from database when changed
    private void checkDatabase(){
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
//                Post post = dataSnapshot.getValue(Post.class);
                // ...
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("dataChanged", "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };
//        mRef.addValueEventListener(postListener);
//        mRef.addListenerForSingleValueEvent();
    }

//    update data
private void writeNewPost(String userId, String username, String title, String body) {
    // Create new post at /user-posts/$userid/$postid and at
    // /posts/$postid simultaneously
//    String key = mRef.child("posts").push().getKey();
//    Post post = new Post(userId, mUsername, title, body);
//    Map<String, Object> postValues = post.toMap();
//
//    Map<String, Object> childUpdates = new HashMap<>();
//    childUpdates.put("/posts/" + key, postValues);
//    childUpdates.put("/user-posts/" + userId + "/" + key, postValues);
//
//    mRef.updateChildren(childUpdates);


}

//order firebase databse
    private void orderTop(){
//        Query myMostViewedPostsQuery = mRef.child("posts").orderByChild("metrics/views");
//        myMostViewedPostsQuery.addChildEventListener(new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) { }
//            // TODO: implement the ChildEventListener methods as documented above
//            // ...
//        });
//
//        //limit list
////        Query recentPostsQuery = databaseReference.child("posts").limitToFirst(100);
//
////        For example, this code queries for the last four items in a Firebase Realtime Database of scores
//
//        DatabaseReference scoresRef = FirebaseDatabase.getInstance().getReference("scores");
//        scoresRef.orderByValue().limitToLast(4).addChildEventListener(new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChild) {
//                Log.d("jhk", "The " + snapshot.getKey() + " dinosaur's score is " + snapshot.getValue());
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) { }

            // ...
//        });
//
//        scoresRef.orderByValue().limitToLast(2).addChildEventListener(new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChild) {
//                Log.d("", "The " + snapshot.getKey() + " dinosaur's score is " + snapshot.getValue());
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) { }
//
//        });
    }

    //if client is disconected
    private void disconected(){
        DatabaseReference presenceRef = FirebaseDatabase.getInstance().getReference("disconnectmessage");
// Write a string when this client loses connection
        presenceRef.onDisconnect().setValue("I disconnected!");

        // to check if disconect is attached

        presenceRef.onDisconnect().removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, @NonNull DatabaseReference reference) {
                if (error != null) {
                    Log.d("", "could not establish onDisconnect event:" + error.getMessage());
                }
            }
        });

        // change that i disconected
        OnDisconnect onDisconnectRef = presenceRef.onDisconnect();
        onDisconnectRef.setValue("I am disconnected");
// ...
// some time later when we change our minds
// ...
        onDisconnectRef.cancel();
    }

    private boolean checkConnection(){
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    Log.d("", "connected");
                    con(true);
                } else {
                    Log.d("", "not connected");
                    con(false);
                }
            }

            private void con(boolean connection){
                connected = connection;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("", "Listener was cancelled");
                con(false);
            }
        });
        return connected;
    }

    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent serviceIntent = new Intent(this, LocationService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                MainActivity.this.startForegroundService(serviceIntent);
            }else{
                startService(serviceIntent);
            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("com.codingwithmitch.googledirectionstest.services.LocationService".equals(service.service.getClassName())) {
                Log.d("service", "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d("service", "isLocationServiceRunning: location service is not running.");
        return false;
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED ) {
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        MY_CAMERA_REQUEST_CODE);
            }
        }
    }

}
