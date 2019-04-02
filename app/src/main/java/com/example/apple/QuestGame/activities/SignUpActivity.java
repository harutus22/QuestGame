package com.example.apple.QuestGame.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.apple.QuestGame.R;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Calendar;
import java.util.UUID;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ImageView userImage;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private TextInputLayout mBirthDate;
    private TextInputLayout mUsername;
    private TextInputLayout mFullame;
    private TextInputEditText mBirthDateField;
    private Button ready;
    private DatePickerDialog date;
    private String previousDate;
    private Button button;
    private String imageName = UUID.randomUUID().toString() + ".jpg";
    private FirebaseStorage storage;
    private LinearLayout layout;
    private Handler handler1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        initFireBase();
        initButtons();

        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                layout.setVisibility(View.VISIBLE);

            }
        },500);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                setUserImage();
//            }
//        });

        ready.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });


    }

    private void initFireBase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        storage = FirebaseStorage.getInstance();
    }

    private void initButtons() {
        mEmail = findViewById(R.id.emailSignUp);
        mPassword = findViewById(R.id.passwordSignUp);
//        mBirthDate = findViewById(R.id.birthdaySignUp);
//        mBirthDateField = findViewById(R.id.birthdaySignUpField);
        mUsername = findViewById(R.id.userNameSignUp);
        mFullame = findViewById(R.id.fullnameSignUp);
        ready = findViewById(R.id.finishSignUp);
        button = findViewById(R.id.button);
        layout = findViewById(R.id.line15);
        handler1 = new Handler();
//        userImage = findViewById(R.id.userImage);

//        mBirthDateField.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Calendar cal = Calendar.getInstance();
//                int day = cal.get(Calendar.DAY_OF_MONTH);
//                int month = cal.get(Calendar.MONTH);
//                int year = cal.get(Calendar.YEAR);
//                datePicker(day, month, year);
//            }
//        });
    }

    private void signUp() {
        if (checkEditText()) {
            mAuth.createUserWithEmailAndPassword(mEmail .getEditText().getText().toString().trim(),
                    mPassword .getEditText().getText().toString().trim())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                User user = new User(imageName, mFullame.getEditText().getText().toString(),
                                        mUsername.getEditText().getText().toString(),
                                        mUsername.getEditText().getText().toString(), mEmail.getEditText().toString());
                                user.getQuests().put("child", new Quest("jdaa", "whay",23, 2166317, 3123131));
                                mDatabase.child("users").child(mAuth.getUid()).setValue(user);
                                logIn();
                            } else {
                                String string = mEmail.getEditText().toString().trim();
                                FirebaseAuthException e = (FirebaseAuthException )task.getException();
                                Toast.makeText(SignUpActivity.this, "Failed Registration: "+e.getMessage(), Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Please fill obligatory fields", Toast.LENGTH_LONG).show();
        return false;
    }

    private void logIn() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void datePicker(int day, int month, int year){
        try {
            previousDate = mBirthDateField.getText().toString();
        } catch (NullPointerException n){}
        date = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                mBirthDateField.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
            }
        }, year, month, day);
        date.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mBirthDateField.setText(previousDate);
            }
        });
        date.show();
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

    private boolean checkUserName(){
        String username = mUsername.getEditText().getText().toString().trim();
        if(username.isEmpty()){
            mUsername.setError("Please enter the username");
            return false;
        } else {
            mUsername.setError(null);
            return true;
        }
    }

    private void setUserImage(){
        onOpenGalleryClick();
    }

    private void onOpenGalleryClick(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == RESULT_OK){
            if(requestCode == Constants.IMAGE_REQUEST){
                Uri imageDataUri = data.getData();
                userImage.setImageURI(imageDataUri);
                uploadToFireBase(imageDataUri);
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
                toast(false);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                toast(true);
            }
        });
    }
    private void toast(boolean success){
        if(!success) {
            Toast.makeText(this, "Can not upload file", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Upload completed", Toast.LENGTH_LONG).show();
        }
    }
}
