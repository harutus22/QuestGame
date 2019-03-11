package com.example.apple.QuestGame.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.models.Quest;
import com.example.apple.QuestGame.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText mEmail;
    private EditText mPassword;
    private EditText mBirthDate;
    private EditText mUsername;
    private EditText mName;
    private EditText mSurname;
    private Button ready;
    private MainActivity mMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mMainActivity = new MainActivity();

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        initButtons();

        ready.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMainActivity.setAuth(mAuth);
                signUp();
            }
        });


    }

    private void initButtons() {
        mEmail = findViewById(R.id.emailSignUp);
        mPassword = findViewById(R.id.passwordSignUp);
        mBirthDate = findViewById(R.id.birthdaySignUp);
        mUsername = findViewById(R.id.userNameSignUp);
        mName = findViewById(R.id.nameSignUp);
        mSurname = findViewById(R.id.surnameSignUp);
        ready = findViewById(R.id.finishSignUp);
    }

    private void signUp() {
        if (checkEditText()) {
            mAuth.createUserWithEmailAndPassword(mEmail.getText().toString(), mPassword.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                User user = new User("", mName.getText().toString() +
                                        mSurname.getText().toString(),mUsername.getText().toString(),
                                        mUsername.getText().toString(), mEmail.getText().toString());
                                user.getQuests().put("child", new Quest("jdaa", "whay",23, 2166317, 3123131));
                                mDatabase.child("users").child(mAuth.getUid()).setValue(user);
//                                mDatabase.child("users").setValue(user);
                                logIn();
                            } else {
                                Toast.makeText(SignUpActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private boolean checkEditText() {
        if (mEmail.getText().toString().isEmpty() && mPassword.getText().toString().isEmpty() &&
                mBirthDate.getText().toString().isEmpty() && mUsername.getText().toString().isEmpty() &&
                mName.getText().toString().isEmpty() && mSurname.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void logIn() {

        Intent intent = new Intent(this, mMainActivity.getClass());
        startActivity(intent);
    }
}
