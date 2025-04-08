package com.example.balancebites;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // Import Handler
import android.os.Looper;  // Import Looper
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.balancebites.AppDatabase;
import com.example.balancebites.User;
import com.example.balancebites.UserDao;
import java.util.concurrent.ExecutorService; // Use ExecutorService
import java.util.concurrent.Executors;   // Use Executors

public class Register extends AppCompatActivity {
    EditText editTextName, editTextEmail, editTextPassword, editTextHeight, editTextWeight, editTextAge;
    AutoCompleteTextView autoCompleteGoal; // Assuming you added this
    Button buttonRegister;
    TextView tvLogin;
    AppDatabase db;
    UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextName = findViewById(R.id.editText); // Use your actual IDs
        editTextEmail = findViewById(R.id.editTextTextEmailAddress);
        editTextPassword = findViewById(R.id.editTextTextPassword);
        editTextHeight = findViewById(R.id.editTextHeight);
        editTextWeight = findViewById(R.id.editTextWeight);
        editTextAge = findViewById(R.id.editTextAge); // Add this EditText to your layout
        autoCompleteGoal = findViewById(R.id.autoCompleteGoal); // Add this AutoCompleteTextView
        buttonRegister = findViewById(R.id.button); // Your register button ID
        tvLogin = findViewById(R.id.tvLogin);

        // Setup Goal dropdown
        String[] goals = {"Maintain", "Lose", "Gain"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, goals);
        autoCompleteGoal.setAdapter(adapter);

        // Get database instance
        db = AppDatabase.getDatabase(getApplicationContext());
        userDao = db.userDao();

        buttonRegister.setOnClickListener(view -> registerUser());

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(Register.this, Login.class);
            startActivity(intent);
        });
    }

    private void registerUser() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String heightStr = editTextHeight.getText().toString().trim();
        String weightStr = editTextWeight.getText().toString().trim();
        String ageStr = editTextAge.getText().toString().trim();
        String goal = autoCompleteGoal.getText().toString().trim();

        // --- Basic Input Validation ---
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(heightStr) || TextUtils.isEmpty(weightStr) || TextUtils.isEmpty(ageStr) || TextUtils.isEmpty(goal)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this,"Enter valid email", Toast.LENGTH_SHORT).show();
            return;
        }
        // Add more validation (password strength, height/weight/age ranges)

        try {
            int heightCm = Integer.parseInt(heightStr);
            int weightKg = Integer.parseInt(weightStr);
            int age = Integer.parseInt(ageStr);

            // --- Calculate BMI and Goal ---
            double bmi = calculateBmi((double) weightKg, heightCm);
            int calorieGoal = calculateCalorieGoal((double) weightKg, heightCm, age, goal);

            // --- Hashing Password (Placeholder - Use a proper library like Bcrypt) ---
            String passwordHash = password; // WARNING: Store a HASH, not plain text!

            User newUser = new User(name, email, passwordHash, heightCm, weightKg, age, goal, bmi, calorieGoal);

            // --- Insert User on Background Thread ---
            ExecutorService executor = AppDatabase.databaseWriteExecutor;
            Handler handler = new Handler(Looper.getMainLooper()); // Handler for UI updates

            executor.execute(() -> {
                // Check if email already exists (optional but good practice)
                User existingUser = userDao.getUserByEmail(email);
                if (existingUser == null) {
                    userDao.insert(newUser);
                    // Post result back to main thread for UI update (Toast/Navigation)
                    handler.post(() -> {
                        Toast.makeText(Register.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(Register.this, Login.class);
                        startActivity(i);
                        finish();
                    });
                } else {
                    handler.post(() -> Toast.makeText(Register.this, "Email already registered", Toast.LENGTH_SHORT).show());
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for height, weight, and age", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Calculation Helpers (Move to a separate utility class later) ---
    private double calculateBmi(double weightKg, int heightCm) {
        if (heightCm <= 0) return 0;
        double heightM = heightCm / 100.0;
        return weightKg / (heightM * heightM);
    }

    private int calculateCalorieGoal(double weightKg, int heightCm, int age, String goal) {
        // Using Average BMR (Mifflin-St Jeor) as gender is omitted
        double bmrMale = (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5;
        double bmrFemale = (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161;
        double bmrAverage = (bmrMale + bmrFemale) / 2.0;

        double tdee = bmrAverage * 1.375; // Assumed Lightly active multiplier

        int calorieAdjustment = 0;
        if ("Lose".equals(goal)) {
            calorieAdjustment = -500;
        } else if ("Gain".equals(goal)) {
            calorieAdjustment = 500;
        } // Maintain has 0 adjustment

        // Ensure goal doesn't go below minimums (basic check)
        int calculatedGoal = (int) Math.round(tdee + calorieAdjustment);
        return Math.max(calculatedGoal, 1200); // Set a basic floor
    }
}