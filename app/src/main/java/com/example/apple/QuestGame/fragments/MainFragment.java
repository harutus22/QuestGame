package com.example.apple.QuestGame.fragments;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
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

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.live_data.PointsLiveData;
import com.example.apple.QuestGame.utils.Constants;
import static android.app.Activity.RESULT_OK;
import static android.support.v4.content.ContextCompat.checkSelfPermission;

public class MainFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private TextView mUsername;
    private TextView mPoints;
    private PointsLiveData model;
    private Button button;
    private ImageView userImage;


    public MainFragment() {
    }

    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = ViewModelProviders.of(getActivity()).get(PointsLiveData.class);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        init(view);
        setUserInfo();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUserImage();
            }
        });
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void setUserInfo() {
        String name = getArguments().getString(ARG_PARAM1);
        String points = getArguments().getString(ARG_PARAM2);
        mUsername.setText(name);
        if (model.getSelected().getValue() == null) {
            mPoints.setText(points);
        } else {
            model.getSelected().observe(this, new Observer<String>() {
                @Override
                public void onChanged(@Nullable String s) {
                    mPoints.setText(s);
                }
            });
        }

    }

    private void init(View container) {
        mUsername = container.findViewById(R.id.mainFragmentUsername);
        mPoints = container.findViewById(R.id.mainFragmentPoints);
        button = container.findViewById(R.id.button);
        userImage = container.findViewById(R.id.userImage);
    }

    @Override
    public void onStop() {
        super.onStop();
        String message = mPoints.getText().toString();
        model.select(message);
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
                userImage.setImageURI(imageDataUri);
            }
        }
    }
}
