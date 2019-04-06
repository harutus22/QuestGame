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
import android.widget.ImageView;
import android.widget.TextView;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.utils.Constants;

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
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void init(View view){
        mUserNameView = view.findViewById(R.id.settings_user_name);
        mPointsView = view.findViewById(R.id.settings_user_points);
        mUserImage = view.findViewById(R.id.settings_user_image);
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
            if (checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
}
