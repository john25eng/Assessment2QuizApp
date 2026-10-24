package com.example.quizapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private RadioButton rbAdmin;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rbAdmin = findViewById(R.id.rbAdmin);

        findViewById(R.id.btnLogin).setOnClickListener(v -> login());
        findViewById(R.id.btnRegister).setOnClickListener(v -> register());

        // already signed in
        if (auth.getCurrentUser() != null) {
            routeByRole(auth.getCurrentUser().getUid());
        }
    }

    private void login() {
        String email = text(etEmail);
        String pass = text(etPassword);
        if (email.isEmpty() || pass.isEmpty()) {
            toast("Enter email and password");
            return;
        }
        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> routeByRole(r.getUser().getUid()))
                .addOnFailureListener(e -> toast("Login failed: " + e.getMessage()));
    }

    private void register() {
        String email = text(etEmail);
        String pass = text(etPassword);
        if (email.isEmpty() || pass.length() < 6) {
            toast("Enter email and a password of 6+ characters");
            return;
        }
        String role = rbAdmin.isChecked() ? "admin" : "player";

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    String uid = r.getUser().getUid();
                    Map<String, Object> user = new HashMap<>();
                    user.put("email", email);
                    user.put("role", role);
                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(x -> routeByRole(uid))
                            .addOnFailureListener(e -> toast("Saving role failed"));
                })
                .addOnFailureListener(e -> toast("Register failed: " + e.getMessage()));
    }

    private void routeByRole(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    Intent i;
                    if ("admin".equals(role)) {
                        i = new Intent(this, AdminHomeActivity.class);
                    } else {
                        i = new Intent(this, PlayerHomeActivity.class);
                    }
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> toast("Could not load role"));
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}