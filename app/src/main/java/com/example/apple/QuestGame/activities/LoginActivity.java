package com.example.apple.QuestGame.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    public static final String TAG = "what";
    private Button mBtnSignIn;
    private Button mBtnSignUp;
    private SignInButton mGoogleSignInButton;
    private LoginButton mFacebookLoginButton;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private EditText mEmail;
    private EditText mPassword;
    private CallbackManager mCallbackManager;
    private boolean mIsLoggedIn;
    private GoogleSignInClient mGoogleSignInClient;
    private MainActivity mMainActivity;

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        //TODO if user logged in, login from this point
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mMainActivity = new MainActivity();

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        mIsLoggedIn = accessToken != null && !accessToken.isExpired();

        googleSignInConfigure();
        initButtons();


        checkIfLogedIn();

        mBtnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
        mBtnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });
        mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInGoogle();
            }
        });
        facebookLogin();
    }

    private void checkIfLogedIn() {

        if (mIsLoggedIn) {
            logIn();
            mMainActivity.setmIsLoggedIn(mIsLoggedIn);
        } else if(googleSignInCheck()){
            logIn();
        } else if(mAuth.getCurrentUser() != null){
            logIn();
        }
        mMainActivity.setmAuth(mAuth);
    }

    private boolean googleSignInCheck() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        return account != null;
    }

    private void signInGoogle(){
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, Constants.GG_SIGN_IN);
    }

    private void googleSignInConfigure() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_id_client))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mMainActivity.setmGoogleSignInClient(mGoogleSignInClient);
    }

    private void facebookLogin() {

        mCallbackManager = CallbackManager.Factory.create();
        mFacebookLoginButton.setReadPermissions("email", "public_profile");
        mFacebookLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {}
            @Override
            public void onError(FacebookException error) {}
        });
    }


    private void logIn() {

        Intent intent = new Intent(this, mMainActivity.getClass());
        startActivity(intent);
        setGooglePlusButtonText("Sign in");
    }

    private void signIn() {
        if (checkEditText()) {
            mAuth.signInWithEmailAndPassword(mEmail.getText().toString(), mPassword.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                logIn();
                            } else {
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void signUp() {
        if (checkEditText()) {
            mAuth.createUserWithEmailAndPassword(mEmail.getText().toString(), mPassword.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                User user = new User("", "Jorj Washington","Jowash",
                                        "user1", mEmail.getText().toString());
                                user.getQuests().put("child", new Quest("jdaa", "whay",23, 2166317, 3123131));
                                mDatabase.child("users").child(user.getUser_id()).setValue(user);
                                logIn();
                            } else {
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
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
                    setGooglePlusButtonText("Sign out");
                } catch (ApiException e) {
                    Toast.makeText(LoginActivity.this, "Authentication failed3.",
                            Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Google sign in failed", e);
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
                            logIn();
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean checkEditText() {
        if (mEmail.getText().toString().isEmpty() && mPassword.getText().toString().isEmpty()) {
            Toast.makeText(this, "Pleas enter login and password", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void initButtons(){
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mBtnSignIn = findViewById(R.id.btnSignIn);
        mBtnSignUp = findViewById(R.id.btnSignUp);
        mGoogleSignInButton = findViewById(R.id.btnGoogleSignIn);
        mGoogleSignInButton.setSize(SignInButton.SIZE_STANDARD);
        mFacebookLoginButton = findViewById(R.id.btnFacebookLogin);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            logIn();
                        } else {
                            Toast.makeText(LoginActivity.this, "Autintification failed 2", Toast.LENGTH_LONG).show();
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


}