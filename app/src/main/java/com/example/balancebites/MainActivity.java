package com.example.balancebites;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button; // Import Button

public class MainActivity extends AppCompatActivity {

    Button buttonGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Your intro layout

        buttonGetStarted = findViewById(R.id.btn); // Use the ID 'btn' from your XML
        buttonGetStarted.setOnClickListener(view -> {
            Intent i = new Intent(MainActivity.this, Login.class); // Go to Login
            startActivity(i);
            finish(); // Optional: Prevent returning to intro
        });
    }
}