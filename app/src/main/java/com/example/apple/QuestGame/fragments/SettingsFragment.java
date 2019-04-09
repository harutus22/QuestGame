package com.example.apple.QuestGame.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.activities.LoginActivity;
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

import static android.app.Activity.RESULT_OK;
import static android.support.v4.content.ContextCompat.checkSelfPermission;

public class SettingsFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mUsername;
    private String mPoints;
    private TextView mUserNameView, mPointsView;
    private ImageView mUserImage;
    private GoogleSignInClient mGoogleSignInClient;
    private Context mContext;
    private Button mSignOut, mEdit;


    public SettingsFragment() { }

    public static SettingsFragment newInstance(String username, String points) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, username);
        args.putString(ARG_PARAM2, points);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUsername = getArguments().getString(ARG_PARAM1);
            mPoints = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
        setViews();
        btnClick();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void init(View view){
        mUserNameView = view.findViewById(R.id.settings_user_name);
        mPointsView = view.findViewById(R.id.settings_user_points);
        mUserImage = view.findViewById(R.id.settings_user_image);
        mEdit = view.findViewById(R.id.settings_btn_edit);
        mSignOut = view.findViewById(R.id.settings_btn_sign_out);
    }

    private void btnClick(){
        mSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOut();
            }
        });
        mEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void setViews(){
        mUserNameView.setText(mUsername);
        mPointsView.setText(mPoints);
    }

    private void setUserImage() {
        onOpenGalleryClick();
    }

    private void onOpenGalleryClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.IMAGE_REQUEST);
            } else {
                getPhoto();
            }
        } else {
            getPhoto();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.IMAGE_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getPhoto();
            }
        }
    }

    private void getPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, Constants.IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == Constants.IMAGE_REQUEST) {
                Uri imageDataUri = data.getData();
                mUserImage.setImageURI(imageDataUri);
            }
        }
    }

    private void logOut() {
        String check = "";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                check = profile.getProviderId();
            }
        }

        switch (check) {
            case "facebook.com":
                signOut();
                LoginManager.getInstance().logOut();
                break;
            case "google.com":
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.google_id_client))
                        .requestEmail().requestId().requestProfile()
                        .build();

                mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);
                mGoogleSignInClient.revokeAccess().addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        signOut();
                        LoginManager.getInstance().logOut();
                    }
                });
                break;
            case "password":
                signOut();
                break;
        }
    }


    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(mContext, LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
    }
}
