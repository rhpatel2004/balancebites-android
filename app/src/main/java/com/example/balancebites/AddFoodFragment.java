package com.example.balancebites;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.balancebites.AppDatabase;
import com.example.balancebites.Food;
import com.example.balancebites.FoodDao;
import com.example.balancebites.FoodLogDao;
import com.example.balancebites.FoodLogEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static android.content.Context.MODE_PRIVATE;

public class AddFoodFragment extends Fragment {

    private EditText editTextSearchFood;
    private RecyclerView recyclerViewFoodItems;
    private FoodAdapter foodAdapter;
    private List<FoodItem> displayedFoodData; // Data currently shown in adapter
    private Button buttonAddCustomFood;
    private AppDatabase db;
    private FoodDao foodDao;
    private FoodLogDao foodLogDao;
    private int userId = -1;
    private Handler handler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        foodDao = db.foodDao();
        foodLogDao = db.foodLogDao();

        // Get user ID from arguments or SharedPreferences
        if (getArguments() != null) {
            userId = getArguments().getInt("USER_ID", -1);
        }
        if (userId == -1) {
            SharedPreferences prefs = requireActivity().getSharedPreferences(Login.PREFS_NAME, MODE_PRIVATE);
            userId = prefs.getInt(Login.PREF_USER_ID, -1);
        }
        if (userId == -1) {
            // Handle error - redirect to login?
            Toast.makeText(getContext(), "Error: User not identified.", Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_food, container, false);

        editTextSearchFood = view.findViewById(R.id.editTextSearchFood);
        recyclerViewFoodItems = view.findViewById(R.id.recyclerViewFoodItems);
        buttonAddCustomFood = view.findViewById(R.id.buttonAddCustomFood);

        displayedFoodData = new ArrayList<>();
        foodAdapter = new FoodAdapter(displayedFoodData, this::logFoodItem);
        recyclerViewFoodItems.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewFoodItems.setAdapter(foodAdapter);

        editTextSearchFood.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { searchFoods(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        buttonAddCustomFood.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddCustomFoodActivity.class);
            startActivity(intent);
        });

        // Load initial/all foods
        searchFoods(""); // Load all initially or recent foods

        return view;
    }

    private void searchFoods(String query) {
        if (userId == -1) return; // Don't search if no user ID

        ExecutorService executor = AppDatabase.databaseWriteExecutor;
        String searchQuery = "%" + query.trim() + "%"; // Prepare for LIKE query

        executor.execute(() -> {
            List<Food> foodsFromDb = foodDao.searchFoodsByName(searchQuery, userId);
            List<FoodItem> foodItems = new ArrayList<>();
            for (Food food : foodsFromDb) {
                foodItems.add(new FoodItem(food.id, food.name, food.servingSize, food.calories)); // Include food.id
            }
            handler.post(() -> {
                if (isAdded()) { // Ensure fragment is still attached
                    displayedFoodData.clear();
                    displayedFoodData.addAll(foodItems);
                    foodAdapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void logFoodItem(FoodItem food) {
        if (userId == -1) {
            Toast.makeText(getContext(), "Error logging food: User not identified.", Toast.LENGTH_SHORT).show();
            return;
        }

        long currentTime = System.currentTimeMillis(); // Get current date/time as timestamp
        FoodLogEntry logEntry = new FoodLogEntry(userId, food.getId(), currentTime, 1.0); // Log 1 serving

        ExecutorService executor = AppDatabase.databaseWriteExecutor;
        executor.execute(() -> {
            foodLogDao.insert(logEntry);
            handler.post(() -> {
                if (isAdded()) {
                    Toast.makeText(getContext(), food.getName() + " logged!", Toast.LENGTH_SHORT).show();
                    // Navigate back to HomeFragment
                    if (getActivity() instanceof HomeActivity) {
                        // ----- CORRECTED METHOD NAME HERE -----
                        ((HomeActivity) getActivity()).replaceFragment(new HomeFragment());
                        // ---------------------------------------

                        // Update bottom nav selection
                        ((HomeActivity) getActivity()).setBottomNavigationItemSelected(R.id.navigation_home);
                    }
                }
            });
        });
    }

    // --- Inner Classes ---
    // FoodItem now includes ID
    public static class FoodItem {
        private int id; private String name, servingSize; private int calories;
        public FoodItem(int id, String name, String serving, int cal) { this.id = id; this.name = name; this.servingSize = serving; this.calories = cal; }
        public int getId() { return id; } // Getter for ID
        public String getName() { return name; }
        public String getServingSize() { return servingSize; }
        public int getCalories() { return calories; }
    }

    // FoodAdapter remains largely the same, uses FoodItem
    public static class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.ViewHolder> {
        private List<FoodItem> foodItems;
        private OnFoodItemClickListener listener;
        public interface OnFoodItemClickListener { void onFoodItemClick(FoodItem food); }

        public FoodAdapter(List<FoodItem> items, OnFoodItemClickListener clickListener) {
            this.foodItems = items; this.listener = clickListener;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_item, parent, false);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FoodItem item = foodItems.get(position);
            holder.foodName.setText(item.getName());
            holder.servingSize.setText(item.getServingSize());
            holder.calories.setText(item.getCalories() + " kcal");
            holder.itemView.setOnClickListener(v -> listener.onFoodItemClick(item));
        }
        @Override public int getItemCount() { return foodItems.size(); }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView foodName, servingSize, calories;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                foodName = itemView.findViewById(R.id.textViewFoodName);
                servingSize = itemView.findViewById(R.id.textViewServingSize);
                calories = itemView.findViewById(R.id.textViewCalories);
            }
        }
    }
}