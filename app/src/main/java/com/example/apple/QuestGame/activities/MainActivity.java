package com.example.apple.QuestGame.activities;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
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
import com.example.apple.QuestGame.fragments.SettingsFragment;
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
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private BottomNavigationView mBottomNavigationView;
    private GoogleSignInClient mGoogleSignInClient;
    private MainFragment mainFragment;
    private String userId;
    private DatabaseReference mRef;
    private String name;
    private String point;
    private PointsLiveData model;
    private String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        model = ViewModelProviders.of(this).get(PointsLiveData.class);
        init();
        getPermissions();
    }

    private void getPermissions() {
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, Constants.PERMISSION_ALL);
        } else {
            startLocationService();
            fireBaseInit();
        }
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
                name = dataSnapshot.child("users").child(userId).child("user_name").getValue(String.class);
                point = String.valueOf(dataSnapshot.child("users").child(userId).child("points").getValue(Long.class));
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

    private void init() {
        mBottomNavigationView = findViewById(R.id.bottom_navigation_view);
        mBottomNavigationView.setOnNavigationItemSelectedListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSION_ALL: {
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationService();
                    fireBaseInit();
                } else {
                    permissionsDialog();
                }
                break;
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        FragmentTransaction fragmentManager = getSupportFragmentManager().beginTransaction();
        if (model.getSelected().getValue() != null) {
            model.getSelected().observe(this, new Observer<String>() {
                @Override
                public void onChanged(@Nullable String s) {
                    point = s;
                }
            });
        }
        switch (menuItem.getItemId()) {


            case R.id.action_map: {
                fragmentManager.replace(R.id.placeHolder, new MapFragment());
                fragmentManager.commit();
            }
            break;

            case R.id.action_quests: {
                MainFragment mainFragment = MainFragment.newInstance(name, point);
                fragmentManager.replace(R.id.placeHolder, mainFragment);
                fragmentManager.commit();
            }
            break;

            case R.id.action_play: {
                fragmentManager.replace(R.id.placeHolder, new CameraFragment());
                fragmentManager.commit();
            }
            break;

            case R.id.action_settings:{
                SettingsFragment settingsFragment = SettingsFragment.newInstance(name, point);
                fragmentManager.replace(R.id.placeHolder, settingsFragment);
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
        switch (item.getItemId()) {
            case R.id.logOut: {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    for (UserInfo profile : user.getProviderData()) {
                        check = profile.getProviderId();
                        Toast.makeText(this, check, Toast.LENGTH_LONG).show();
                    }
                }

                if (check.equals("facebook.com")) {
                    signOut();
                    LoginManager.getInstance().logOut();
                } else if (check.equals("google.com")) {
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
                } else if (check.equals("password")) {
                    signOut();
                }
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
    }


    private void getUserInfo() {
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
    private void checkDatabase() {
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

    //order firebase databse
    private void orderTop() {
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

    private void startLocationService() {
        if (!isLocationServiceRunning()) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                MainActivity.this.startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.codingwithmitch.googledirectionstest.services.LocationService".equals(service.service.getClassName())) {
                Log.d("service", "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d("service", "isLocationServiceRunning: location service is not running.");
        return false;
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void permissionsDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This app requires permissions to be applied \n Do you want to apply them?")
                .setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getPermissions();
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishAffinity();
                System.exit(0);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
