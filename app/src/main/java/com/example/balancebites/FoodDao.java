package com.example.balancebites;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Food food);

    @Query("SELECT * FROM foods WHERE id = :foodId LIMIT 1")
    Food getFoodById(int foodId);

    // Search both general foods (userId is null) and user's custom foods
    @Query("SELECT * FROM foods WHERE name LIKE :query AND (userId IS NULL OR userId = :userId)")
    List<Food> searchFoodsByName(String query, int userId);

    @Query("SELECT * FROM foods WHERE userId IS NULL OR userId = :userId ORDER BY name ASC")
    List<Food> getAllFoodsForUser(int userId); // Gets general + user's custom

    // Add delete/update methods if needed
}