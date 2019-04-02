package com.example.apple.QuestGame.activities;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.models.Quest;
import com.example.apple.QuestGame.models.User;
import com.example.apple.QuestGame.utils.Constants;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private Button mBtnSignIn;
    private SignInButton mGoogleSignInButton;
    private LoginButton mFacebookLoginButton;
    private TextView mSignUp;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private CallbackManager mCallbackManager;
    private boolean mIsLoggedIn;
    private GoogleSignInClient mGoogleSignInClient;
    private ImageView logoFb;
    private ImageView logoGoogle;
    private LinearLayout layout;
    private Handler handler;
    private ImageView imageView;
    private List<String> list = new ArrayList<>();
    private final String authFailed = "Authentication failed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        list.add("email");
        list.add("public_profile");
        if(checkConnection()) {

            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference();

            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            mIsLoggedIn = accessToken != null && !accessToken.isExpired();

            googleSignInConfigure();
            initButtons();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    layout.setVisibility(View.VISIBLE);
                }
            },3000);
            checkIfLogedIn();

            mBtnSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signIn();
                }
            });
            mSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signUp();
                }
            });
            logoGoogle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signInGoogle();
                }
            });
            facebookLogin();
        }
    }

    private void checkIfLogedIn() {

        if (mIsLoggedIn) {
            logIn();
        } else if (googleSignInCheck()) {
            logIn();
        } else if (mAuth.getCurrentUser() != null) {
            logIn();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(checkConnection()) {
            setGooglePlusButtonText("Sign in");
        }
    }

    private boolean googleSignInCheck() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        return account != null;
    }

    private void signInGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, Constants.GG_SIGN_IN);
    }

    private void googleSignInConfigure() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_id_client))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void facebookLogin() {

        mCallbackManager = CallbackManager.Factory.create();
//        mFacebookLoginButton.setReadPermissions("email", "public_profile");
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException error) {
            }
        });


        logoFb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this,list);
            }
        });
    }


    private void logIn() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void signIn() {
        if (checkEmail() && checkPassword()) {
            mAuth.signInWithEmailAndPassword(mEmail.getEditText()
                    .getText().toString(), mPassword.getEditText().getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                logIn();
                            } else {
                                Toast.makeText(LoginActivity.this, authFailed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void signUp() {
        Intent signUp = new Intent(this, SignUpActivity.class);
        startActivity(signUp);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.GG_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(LoginActivity.this, authFailed,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleFacebookAccessToken(AccessToken token) {

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            createUser(task);
                            logIn();
                        } else {
                            Toast.makeText(LoginActivity.this, authFailed,
                                    Toast.LENGTH_SHORT).show();
                            LoginManager.getInstance().logOut();
                            FirebaseAuth.getInstance().signOut();
                        }
                    }
                });
    }

    private void createUser(final Task<AuthResult> task) {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.child("users").child(mAuth.getUid()).exists()) {
                    User user = new User("", task.getResult().getUser().getDisplayName(), "User",
                            task.getResult().getUser().getUid(), task.getResult().getUser().getEmail());
                    user.getQuests().put("child", new Quest("", "", 0, 0, 0));
                    mDatabase.child("users").child(mAuth.getUid()).setValue(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private boolean checkEmail() {
        String email = mEmail.getEditText().getText().toString().trim();
        if (email.isEmpty()) {
            mEmail.setError("Please enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmail.setError("Please enter a valid email address");
            return false;
        } else {
            mEmail.setError(null);
            return true;
        }

    }

    private boolean checkPassword(){
        String password = mPassword.getEditText().getText().toString().trim();
        if (password.isEmpty()) {
            mPassword.setError("Please enter password");
            return false;
        } else {
            mPassword.setError(null);
            return true;
        }
    }

    private void initButtons() {
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mBtnSignIn = findViewById(R.id.btnSignIn);
        mGoogleSignInButton = findViewById(R.id.btnGoogleSignIn);
        mFacebookLoginButton = findViewById(R.id.btnFacebookLogin);
        mSignUp = findViewById(R.id.signUpTextView);
        layout = (LinearLayout) findViewById(R.id.line11);
        handler = new Handler();
        imageView = findViewById(R.id.logo);
        logoFb = findViewById(R.id.fb_logo);
        logoGoogle = findViewById(R.id.google_logo);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            setGooglePlusButtonText("Sign out");
                            createUser(task);
                            logIn();
                        } else {
                            Toast.makeText(LoginActivity.this, authFailed, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    protected void setGooglePlusButtonText(String text) {
        for (int i = 0; i < mGoogleSignInButton.getChildCount(); i++) {
            View v = mGoogleSignInButton.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setText(text);
                return;
            }
        }
    }

    private boolean checkConnection(){
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        if(isConnected) {
            Log.d("Network", "Connected");
            return true;
        }
        else{
            noInternetDialog();
            Log.d("Network","Not Connected");
            return false;
        }
    }

    private void noInternetDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No internet Connection");
        builder.setMessage("Please turn on internet connection and start app");
        builder.setNegativeButton("close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}