package com.example.balancebites;

import android.content.Intent;
import android.content.SharedPreferences; // Import SharedPreferences
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.balancebites.AppDatabase;
import com.example.balancebites.User;
import com.example.balancebites.UserDao;
import java.util.concurrent.ExecutorService;

public class Login extends AppCompatActivity {

    Button buttonLogin;
    TextView tvRegister;
    EditText editTextEmail;
    EditText editTextPassword;
    AppDatabase db;
    UserDao userDao;

    // Simple session management using SharedPreferences
    public static final String PREFS_NAME = "BalanceBitesPrefs";
    public static final String PREF_USER_ID = "LoggedInUserId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int loggedInUserId = prefs.getInt(PREF_USER_ID, -1); // -1 means not logged in
        if (loggedInUserId != -1) {
            goToHomeActivity(loggedInUserId); // Go directly to HomeActivity
            return; // Skip setting login layout
        }


        setContentView(R.layout.activity_login);

        buttonLogin = findViewById(R.id.button);
        tvRegister = findViewById(R.id.tvRegister);
        editTextEmail = findViewById(R.id.editTextTextEmailAddress);
        editTextPassword = findViewById(R.id.editTextTextPassword);

        db = AppDatabase.getDatabase(getApplicationContext());
        userDao = db.userDao();

        buttonLogin.setOnClickListener(view -> loginUser());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Register.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        ExecutorService executor = AppDatabase.databaseWriteExecutor;
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            User user = userDao.getUserByEmail(email);

            handler.post(() -> {
                // --- Password Check (Placeholder - Compare HASHES!) ---
                if (user != null && user.passwordHash.equals(password)) { // WARNING: Check HASHES
                    Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_SHORT).show();

                    // Save user ID in SharedPreferences
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putInt(PREF_USER_ID, user.id);
                    editor.apply();

                    goToHomeActivity(user.id);

                } else {
                    Toast.makeText(Login.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    private void goToHomeActivity(int userId) {
        Intent i = new Intent(Login.this, HomeActivity.class);
        i.putExtra("USER_ID", userId); // Pass user ID to HomeActivity
        startActivity(i);
        finish();
    }
}