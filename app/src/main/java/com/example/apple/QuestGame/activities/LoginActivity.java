package com.example.apple.QuestGame.activities;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    private final String authFailed = "Authentication failed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

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
//        mBtnSignUp.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                signUp();
//            }
//        });
        mSignUp.setOnClickListener(new View.OnClickListener() {
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
        } else if (googleSignInCheck()) {
            logIn();
        } else if (mAuth.getCurrentUser() != null) {
            logIn();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setGooglePlusButtonText("Sign in");
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
        mFacebookLoginButton.setReadPermissions("email", "public_profile");
        mFacebookLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
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
    }


    private void logIn() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void signIn() {
        if (checkEditText()) {
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

    private boolean checkEditText() {
        String email = mEmail.getEditText().getText().toString().trim();
        if (email.isEmpty()) {
            mEmail.setError("Please enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmail.setError("Please enter a valid email address");
            return false;
        } else if (mPassword.getEditText().getText().toString().isEmpty()) {
            mPassword.setError("Please enter password");
            return false;
        }
        mEmail.setError(null);
        mPassword.setError(null);
        return true;
    }

    private void initButtons() {
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mBtnSignIn = findViewById(R.id.btnSignIn);
        mGoogleSignInButton = findViewById(R.id.btnGoogleSignIn);
        mFacebookLoginButton = findViewById(R.id.btnFacebookLogin);
        mSignUp = findViewById(R.id.signUpTextView);
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


}