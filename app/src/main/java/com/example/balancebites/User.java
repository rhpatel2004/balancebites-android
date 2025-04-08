package com.example.balancebites; // Create a 'room' sub-package

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore; // Import Ignore

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String username; // Added username field
    public String email;
    public String passwordHash; // Store HASHED password
    public int heightCm;
    public int weightKg;
    public int age;
    // public String sex; // Removed as per previous discussion
    public String goal; // "Gain", "Lose", "Maintain"
    public double bmi;
    public int dailyCalorieGoal;

    // Room requires a public no-arg constructor or one where all args match fields
    public User() {}

    // Constructor for easier object creation (ignored by Room)
    @Ignore
    public User(String username, String email, String passwordHash, int heightCm, int weightKg, int age, String goal, double bmi, int dailyCalorieGoal) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.age = age;
        this.goal = goal;
        this.bmi = bmi;
        this.dailyCalorieGoal = dailyCalorieGoal;
    }
}