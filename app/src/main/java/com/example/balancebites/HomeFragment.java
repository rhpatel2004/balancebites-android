package com.example.balancebites; // Make sure this package name is correct
// --- CORRECTED IMPORTS ---
// --- PASTE THIS ENTIRE BLOCK BELOW YOUR PACKAGE DECLARATION ---
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Imports for your database classes (assuming they are in the main package)
import com.example.balancebites.AppDatabase;
import com.example.balancebites.Food;
import com.example.balancebites.FoodDao;
import com.example.balancebites.FoodLogDao;
import com.example.balancebites.FoodLogEntry;
import com.example.balancebites.User;
import com.example.balancebites.UserDao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static android.content.Context.MODE_PRIVATE;
// --- END OF IMPORTS ---
public class HomeFragment extends Fragment {

    private TextView textViewCalories;
    private TextView textViewGoal;
    private TextView textViewRemaining;
    private ProgressBar progressBar;
    private RecyclerView recyclerViewHistory;
    private HistoryAdapter historyAdapter;
    private List<HistoryItem> historyData;
    private Button buttonLogFoodHome;

    private AppDatabase db;
    private UserDao userDao;
    private FoodDao foodDao;
    private FoodLogDao foodLogDao;
    private int userId = -1;
    private int userGoalCalories = 2000; // Default goal
    private Handler handler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        // Use requireContext() for non-null context after onCreate
        db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        userDao = db.userDao();
        foodDao = db.foodDao();
        foodLogDao = db.foodLogDao();

        // Get user ID from arguments passed by HomeActivity
        if (getArguments() != null) {
            userId = getArguments().getInt("USER_ID", -1);
        }
        // Fallback to SharedPreferences if arguments are null
        if (userId == -1) {
            SharedPreferences prefs = requireActivity().getSharedPreferences(Login.PREFS_NAME, MODE_PRIVATE);
            userId = prefs.getInt(Login.PREF_USER_ID, -1);
        }
        if (userId == -1) {
            Toast.makeText(getContext(), "Error: User not identified. Please login again.", Toast.LENGTH_LONG).show();
            // Consider navigating back to Login
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        textViewCalories = view.findViewById(R.id.textViewCalories);
        textViewGoal = view.findViewById(R.id.textViewGoal);
        textViewRemaining = view.findViewById(R.id.textViewRemaining);
        progressBar = view.findViewById(R.id.progressBar);
        recyclerViewHistory = view.findViewById(R.id.recyclerViewHistory);
        buttonLogFoodHome = view.findViewById(R.id.buttonLogFoodHome); // Use correct ID from fragment_home.xml

        historyData = new ArrayList<>();
        historyAdapter = new HistoryAdapter(historyData); // Initialize adapter
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewHistory.setAdapter(historyAdapter);

        buttonLogFoodHome.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).replaceFragment(new AddFoodFragment());
                ((HomeActivity) getActivity()).setBottomNavigationItemSelected(R.id.navigation_add_food);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUserDataAndTodaysLog(); // Load data when view is ready
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to the fragment
        loadUserDataAndTodaysLog();
    }

    private void loadUserDataAndTodaysLog() {
        if (userId == -1 || getContext() == null) return;

        ExecutorService executor = AppDatabase.databaseWriteExecutor;
        executor.execute(() -> {
            // Get User Goal
            User user = userDao.getUserById(userId);
            if (user != null) {
                userGoalCalories = user.dailyCalorieGoal;
            } else {
                userGoalCalories = 2000; // Reset to default if user fetch fails
            }

            // Get Today's Calories
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            long endOfDay = cal.getTimeInMillis();

            int consumedToday = foodLogDao.getTotalCaloriesForDate(userId, startOfDay, endOfDay);

            // Update UI on Main Thread
            int finalGoal = userGoalCalories;
            handler.post(() -> {
                if (isAdded()) {
                    updateCalorieSummary(consumedToday, finalGoal);
                }
            });

            loadHistoryData(); // Load history after getting user goal
        });
    }

    private void loadHistoryData() {
        if (userId == -1 || getContext() == null) return;

        ExecutorService executor = AppDatabase.databaseWriteExecutor;
        executor.execute(() -> {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -7); // History start date
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
            long historyStartDate = cal.getTimeInMillis();

            List<FoodLogEntry> recentEntries = foodLogDao.getRecentLogEntries(userId, historyStartDate);
            List<Food> allFoods = foodDao.getAllFoodsForUser(userId);

            Map<String, Integer> dailyTotals = new HashMap<>();
            // Use format with year for reliable grouping/sorting
            SimpleDateFormat groupingSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displaySdf = new SimpleDateFormat("dd MMM", Locale.getDefault());

            Map<Integer, Integer> foodCalorieMap = new HashMap<>();
            for (Food food : allFoods) {
                foodCalorieMap.put(food.id, food.calories);
            }

            String todayGroupingStr = groupingSdf.format(new Date());

            for (FoodLogEntry entry : recentEntries) {
                String dayKey = groupingSdf.format(new Date(entry.date));
                if (dayKey.equals(todayGroupingStr)) continue; // Skip today

                int caloriesForEntry = 0;
                Integer foodCalories = foodCalorieMap.get(entry.foodId);
                if (foodCalories != null) {
                    caloriesForEntry = (int) Math.round(foodCalories * entry.quantity);
                }

                // API Level 21 compatible way to update map
                int currentTotal = dailyTotals.containsKey(dayKey) ? dailyTotals.get(dayKey) : 0;
                dailyTotals.put(dayKey, currentTotal + caloriesForEntry);
            }

            List<HistoryItem> newHistoryData = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : dailyTotals.entrySet()) {
                try {
                    Date entryDate = groupingSdf.parse(entry.getKey());
                    if (entryDate != null) { // Check if parsing succeeded
                        newHistoryData.add(new HistoryItem(displaySdf.format(entryDate), entry.getValue(), entryDate)); // Store date object
                    }
                } catch (ParseException e) {
                    e.printStackTrace(); // Log error
                }
            }

            // Sort history data by date descending
            Collections.sort(newHistoryData, (o1, o2) -> o2.getSortableDate().compareTo(o1.getSortableDate())); // Use stored date object

            // Update UI on Main Thread
            handler.post(() -> {
                if (isAdded()) {
                    historyData.clear();
                    historyData.addAll(newHistoryData);
                    historyAdapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void updateCalorieSummary(int consumed, int goal) {
        if (textViewCalories == null || getContext() == null) return;

        textViewCalories.setText(consumed + " kcal");
        textViewGoal.setText("/ " + goal + " kcal");
        int remaining = goal - consumed;
        textViewRemaining.setText(remaining + " kcal Remaining");

        if (remaining < 0) {
            textViewRemaining.setText(Math.abs(remaining) + " kcal Over");
            textViewRemaining.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        } else {
            textViewRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_500));
        }
        int progress = (goal > 0) ? (int)(((float)consumed / goal) * 100) : 0;
        progressBar.setProgress(Math.min(progress, 100));
    }

    // --- Inner Class for History Item (Added Date object for sorting) ---
    public static class HistoryItem {
        private String displayDate; // e.g., "26 Mar"
        private int calories;
        private Date sortableDate; // The actual date for sorting

        public HistoryItem(String displayDate, int calories, Date sortableDate) {
            this.displayDate = displayDate;
            this.calories = calories;
            this.sortableDate = sortableDate;
        }

        public String getDate() { // Keep original method name for adapter
            return displayDate;
        }

        public int getCalories() {
            return calories;
        }

        public Date getSortableDate() { // Getter for sorting
            return sortableDate;
        }
    }

    // --- Inner Class for HistoryAdapter (Corrected and Complete) ---
    public static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<HistoryItem> historyItems;

        // ViewHolder definition MUST be inside or accessible
        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView date, calories;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                date = itemView.findViewById(R.id.history_date);
                calories = itemView.findViewById(R.id.history_calories);
            }
        }

        // Constructor for the adapter
        public HistoryAdapter(List<HistoryItem> historyItems) {
            this.historyItems = historyItems;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Correctly inflate the item layout
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
            return new ViewHolder(view); // Return the ViewHolder
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // Bind data to the ViewHolder
            HistoryItem item = historyItems.get(position);
            holder.date.setText(item.getDate()); // Use getDate() from HistoryItem
            holder.calories.setText(item.getCalories() + " kcal"); // Use getCalories()
        }

        @Override
        public int getItemCount() {
            // Return the size of the list
            return historyItems.size();
        }
    }
}