package com.example.balancebites;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "food_log",
        foreignKeys = {
                @ForeignKey(entity = User.class,
                        parentColumns = "id",
                        childColumns = "userId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Food.class,
                        parentColumns = "id",
                        childColumns = "foodId",
                        onDelete = ForeignKey.CASCADE)
        })
public class FoodLogEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public int foodId;
    public long date; // Unix timestamp (milliseconds)
    public double quantity; // e.g., 1.0, 0.5, 2.0 servings

    public FoodLogEntry() {}

    @Ignore
    public FoodLogEntry(int userId, int foodId, long date, double quantity) {
        this.userId = userId;
        this.foodId = foodId;
        this.date = date;
        this.quantity = quantity;
    }
}