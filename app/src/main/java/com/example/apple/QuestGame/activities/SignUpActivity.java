package com.example.apple.QuestGame.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
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
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private TextInputLayout mBirthDate;
    private TextInputLayout mUsername;
    private TextInputLayout mFullame;
    private Button ready;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        initButtons();

        ready.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });


    }

    private void initButtons() {
        mEmail = findViewById(R.id.emailSignUp);
        mPassword = findViewById(R.id.passwordSignUp);
        mBirthDate = findViewById(R.id.birthdaySignUp);
        mUsername = findViewById(R.id.userNameSignUp);
        mFullame = findViewById(R.id.fullnameSignUp);
        ready = findViewById(R.id.finishSignUp);
    }

    private void signUp() {
        if (checkEditText()) {
            mAuth.createUserWithEmailAndPassword(mEmail.getEditText().getText().toString(), mPassword.getEditText().getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                User user = new User("", mFullame.getEditText().getText().toString(),
                                        mUsername.getEditText().getText().toString(),
                                        mUsername.getEditText().getText().toString(), mEmail.getEditText().getText().toString());
                                user.getQuests().put("child", new Quest("jdaa", "whay",23, 2166317, 3123131));
                                mDatabase.child("users").child(mAuth.getUid()).setValue(user);
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
        if (mEmail.getEditText().toString().isEmpty() && mPassword.getEditText().toString().isEmpty() &&
                mBirthDate.getEditText().toString().isEmpty() && mUsername.getEditText().toString().isEmpty() &&
                mFullame.getEditText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void logIn() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
