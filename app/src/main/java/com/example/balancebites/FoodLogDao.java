package com.example.balancebites;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FoodLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FoodLogEntry logEntry);

    // Get log entries for a specific user on a specific date range
    @Query("SELECT * FROM food_log WHERE userId = :userId AND date >= :startDate AND date < :endDate ORDER BY date DESC")
    List<FoodLogEntry> getLogEntriesForDateRange(int userId, long startDate, long endDate);

    // Query to sum calories for a specific day (requires joining Food table)
    @Query("SELECT SUM(f.calories * fl.quantity) FROM food_log fl " +
            "JOIN foods f ON fl.foodId = f.id " +
            "WHERE fl.userId = :userId AND fl.date >= :startDate AND fl.date < :endDate")
    int getTotalCaloriesForDate(int userId, long startDate, long endDate);

    // Query for history (simplified - gets total calories per day for last N days)
    // NOTE: Grouping by date directly from timestamp needs careful handling in SQL
    // This simplified query gets recent entries, grouping needs to be done in code
    @Query("SELECT * FROM food_log WHERE userId = :userId AND date >= :startDate ORDER BY date DESC")
    List<FoodLogEntry> getRecentLogEntries(int userId, long startDate);

    // Add delete/update methods if needed
}