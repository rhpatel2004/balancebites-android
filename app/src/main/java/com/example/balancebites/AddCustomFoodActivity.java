package com.example.balancebites;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.balancebites.AppDatabase;
import com.example.balancebites.Food;
import com.example.balancebites.FoodDao;
import java.util.concurrent.ExecutorService;

public class AddCustomFoodActivity extends AppCompatActivity {

    EditText editTextFoodName, editTextServingSize, editTextCalories;
    Button buttonSaveFood;
    AppDatabase db;
    FoodDao foodDao;
    int userId = -1;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_custom_food);

        editTextFoodName = findViewById(R.id.editTextFoodName);
        editTextServingSize = findViewById(R.id.editTextServingSize);
        editTextCalories = findViewById(R.id.editTextCalories);
        buttonSaveFood = findViewById(R.id.buttonSaveFood);

        db = AppDatabase.getDatabase(getApplicationContext());
        foodDao = db.foodDao();
        handler = new Handler(Looper.getMainLooper());

        // Get logged-in user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(Login.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt(Login.PREF_USER_ID, -1);
        if (userId == -1) {
            // Handle error - should not happen if user is logged in
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_LONG).show();
            finish(); // Close activity if no user ID
            return;
        }

        buttonSaveFood.setOnClickListener(v -> saveCustomFood());
    }

    private void saveCustomFood() {
        String name = editTextFoodName.getText().toString().trim();
        String servingSize = editTextServingSize.getText().toString().trim();
        String caloriesStr = editTextCalories.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(servingSize) || TextUtils.isEmpty(caloriesStr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int calories = Integer.parseInt(caloriesStr);
            Food newFood = new Food(userId, name, servingSize, calories); // Pass userId

            ExecutorService executor = AppDatabase.databaseWriteExecutor;
            executor.execute(() -> {
                foodDao.insert(newFood);
                handler.post(() -> {
                    Toast.makeText(AddCustomFoodActivity.this, "Custom food saved!", Toast.LENGTH_SHORT).show();
                    finish(); // Close this activity after saving
                });
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for calories", Toast.LENGTH_SHORT).show();
        }
    }
}