package com.example.apple.QuestGame.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.activities.MainActivity;
import com.example.apple.QuestGame.models.Quest;
import com.example.apple.QuestGame.models.User;
import com.example.apple.QuestGame.utils.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

public class SignUpFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ImageView userImage;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private TextInputLayout mUsername;
    private TextInputLayout mFullame;
    private Button ready, chooseImage;
    private DatePickerDialog date;
    private String previousDate;
    private String imageName = UUID.randomUUID().toString() + ".pngx";
    private FirebaseStorage storage;
    private boolean available, mailAvailable;
    private TextInputEditText userNameField;
    private TextInputEditText userEmailField;
    private TextInputEditText userPasswordField;
    private Context mContext;
    private Activity mActivity;



    public SignUpFragment() { }

    public static SignUpFragment newInstance(String param1, String param2) {
        SignUpFragment fragment = new SignUpFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFireBase();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initButtons(view);
        checkInputedInfo();

        ready.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });
        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUserImage();
            }
        });
    }

    private void initFireBase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        storage = FirebaseStorage.getInstance();
    }

    private void initButtons(View view) {
        mEmail = view.findViewById(R.id.emailSignUp);
        mPassword = view.findViewById(R.id.passwordSignUp);
        mUsername = view.findViewById(R.id.userNameSignUp);
        mFullame = view.findViewById(R.id.fullnameSignUp);
        ready = view.findViewById(R.id.finishSignUp);
        chooseImage = view.findViewById(R.id.signUpPhotoUpload);
        userImage = view.findViewById(R.id.signUpUserImage);
        userNameField = view.findViewById(R.id.userNameField);
        userEmailField = view.findViewById(R.id.userEmailField);
        userPasswordField = view.findViewById(R.id.userPasswordField);
    }

    private void signUp() {
        if (checkEditText()) {
            mAuth.createUserWithEmailAndPassword(mEmail .getEditText().getText().toString().trim(),
                    mPassword .getEditText().getText().toString().trim())
                    .addOnCompleteListener(mActivity, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                User user = new User(imageName, mFullame.getEditText().getText().toString(),
                                        mUsername.getEditText().getText().toString(),
                                        mUsername.getEditText().getText().toString(), mEmail.getEditText().getText().toString());
                                user.getQuests().put("child", new Quest());
                                mDatabase.child("users").child(mAuth.getUid()).setValue(user);
                                logIn();
                            } else {
                                String string = mEmail.getEditText().toString().trim();
                                FirebaseAuthException e = (FirebaseAuthException )task.getException();
                                Toast.makeText(mContext, "Failed Registration: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("SignUpActivity", "Failed Registration", e);
                                Log.e("textMail", string);
                            }
                        }
                    });
        }
    }

    private boolean checkEditText() {
        if (checkEmail() && checkPassword() && checkUserName()) {
            return true;
        }
        Toast.makeText(mContext, "Please fill obligatory fields", Toast.LENGTH_LONG).show();
        return false;
    }

    private void logIn() {
        uploadToFireBase(getImageUri(mContext, getBitmap()));
        Intent intent = new Intent(mActivity, MainActivity.class);
        startActivity(intent);
        mActivity.finish();
    }

    private boolean checkEmail() {
        String email = mEmail.getEditText().getText().toString().trim();
        checkEmailDatabase(email);
        if (email.isEmpty()) {
            mEmail.setError("Please enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmail.setError("Please enter a valid email address");
            return false;
        } else if(!mailAvailable){
            mEmail.setError("Email is occupied");
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
        } else if (password.length() < 6) {
            mPassword.setError("Must be longer than 6 letter");
            return false;
        } else {
            mPassword.setError(null);
            return true;
        }
    }

    private boolean checkUserName(){
        String username = mUsername.getEditText().getText().toString().trim();
        checkUserNameAvailability(username);
        if(username.isEmpty()){
            mUsername.setError("Please enter the username");
            return false;
        } else if(!available){
            mUsername.setError("Username is occupied ");
            return false;
        }else if(username.length() < 6){
            mUsername.setError("Must be longer than 6 letter");
            return false;
        }
        else
        {
            mUsername.setError(null);
            return true;
        }
    }

    private void setUserImage(){
        onOpenGalleryClick();
    }

    private void onOpenGalleryClick(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(mContext.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
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
        if(requestCode == Constants.IMAGE_REQUEST){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
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

        if(resultCode == RESULT_OK){
            if(requestCode == Constants.IMAGE_REQUEST){
                Uri imageDataUri = data.getData();
                userImage.setImageBitmap(resizeImage(imageDataUri));
            }
        }
    }

    private void uploadToFireBase(Uri imageUri) {
        StorageReference storageRef = storage.getReference();
        StorageReference riversRef = storageRef.child("images").child(imageName);
        UploadTask uploadTask = riversRef.putFile(imageUri);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("firebaseException", exception.getMessage());
                toast(false);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                toast(true);
                if(!checkImageAvailability(imageName)){
                    saveImageToStorage(getBitmap(), imageName);
                }
            }
        });
    }
    private void toast(boolean success){
        if(!success) {
            Toast.makeText(mContext, "Can not upload file", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mContext, "Upload completed", Toast.LENGTH_LONG).show();
        }
    }

    private void checkUserNameAvailability(final String string){
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot dataSnapshot1: dataSnapshot.child("users").getChildren()){
                    String check = dataSnapshot1.child("user_name").getValue(String.class);
                    if(!string.equals(check)){
                        available = true;
                    } else {
                        available = false;
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        }, 2000);
    }

    private Bitmap resizeImage(Uri imgFileOrig){
        Bitmap b = null;
        try {
            b = MediaStore.Images.Media.getBitmap(mActivity.getContentResolver(), imgFileOrig);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int origWidth = b.getWidth();
        int origHeight = b.getHeight();

        final int destWidth = 400;

        if (origWidth > destWidth) {
            int destHeight = origHeight / (origWidth / destWidth);
            Bitmap b2 = Bitmap.createScaledBitmap(b, destWidth, destHeight, false);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            b2.compress(Bitmap.CompressFormat.PNG, 100, outStream);

            File f = new File(Environment.getExternalStorageDirectory()
                    + File.separator + ".png");
            try {
                f.createNewFile();
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(outStream.toByteArray());
                fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return b2;
        } else {
            return b;
        }
    }

    private Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private Bitmap getBitmap(){
        userImage.invalidate();
        BitmapDrawable drawable = (BitmapDrawable) userImage.getDrawable();
        return drawable.getBitmap();
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
            MediaStore.Images.Media.insertImage(mActivity.getContentResolver(),file.getAbsolutePath()
                    ,file.getName(),file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getFile(String imageName){
        String path = Environment.getExternalStorageDirectory().toString();
        Log.d("path", path);
        return new File(path, imageName + ".png");
    }

    private boolean checkImageAvailability(String imageNAme){
        File file = getFile(imageNAme);
        return file.exists();
    }

    private void checkInputedInfo() {
        userNameField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    if (mUsername.getEditText().getText().length() != 0) {
                        checkUserName();
                    }
                }
            }
        });
        userEmailField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    if (mEmail.getEditText().getText().length() != 0) {
                        checkEmail();
                    }
                }
            }
        });
        userPasswordField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    if (mPassword.getEditText().getText().length() != 0) {
                        checkPassword();
                    }
                }
            }
        });
    }

    private void checkEmailDatabase(final String mail){
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot dataSnapshot1: dataSnapshot.child("users").getChildren()){
                    String check = dataSnapshot1.child("mail").getValue(String.class);
                    if(!mail.equals(check)){
                        mailAvailable = true;
                    } else {
                        mailAvailable = false;
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
