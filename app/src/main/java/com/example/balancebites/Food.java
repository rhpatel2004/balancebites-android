package com.example.balancebites;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "foods")
public class Food {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public Integer userId; // Use Integer (nullable) for FK. null = general food
    public String name;
    public String servingSize;
    public int calories;

    public Food() {}

    @Ignore
    public Food(Integer userId, String name, String servingSize, int calories) {
        this.userId = userId;
        this.name = name;
        this.servingSize = servingSize;
        this.calories = calories;
    }
}