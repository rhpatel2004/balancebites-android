package com.example.balancebites;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.balancebites.AppDatabase;
import com.example.balancebites.User;
import com.example.balancebites.UserDao;
import java.util.concurrent.ExecutorService;

import static android.content.Context.MODE_PRIVATE; // Import MODE_PRIVATE

public class ProfileFragment extends Fragment {

    private TextView textViewName, textViewEmail, textViewHeight, textViewWeight, textViewAge, textViewGoalCalorie;
    private Button buttonLogout;
    private AppDatabase db;
    private UserDao userDao;
    private int userId = -1;
    private Handler handler; // Handler for UI updates

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper()); // Initialize handler
        db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        userDao = db.userDao();
        // Get user ID passed from HomeActivity
        if (getArguments() != null) {
            userId = getArguments().getInt("USER_ID", -1);
        }
        if (userId == -1) {
            // Fallback or error handling if ID is not found
            SharedPreferences prefs = requireActivity().getSharedPreferences(Login.PREFS_NAME, MODE_PRIVATE);
            userId = prefs.getInt(Login.PREF_USER_ID, -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        textViewName = view.findViewById(R.id.textViewName);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        textViewHeight = view.findViewById(R.id.textViewHeight);
        textViewWeight = view.findViewById(R.id.textViewWeight);
        textViewAge = view.findViewById(R.id.textViewAge);
        textViewGoalCalorie = view.findViewById(R.id.textViewGoalCalorie);
        buttonLogout = view.findViewById(R.id.buttonLogout);

        loadUserProfile(); // Load data from DB

        buttonLogout.setOnClickListener(v -> logoutUser());

        return view;
    }

    private void loadUserProfile() {
        if (userId != -1) {
            ExecutorService executor = AppDatabase.databaseWriteExecutor;
            executor.execute(() -> {
                User user = userDao.getUserById(userId);
                handler.post(() -> {
                    if (user != null && isAdded()) { // Check if fragment is still added
                        textViewName.setText(user.username);
                        textViewEmail.setText(user.email);
                        textViewHeight.setText(user.heightCm + "cm");
                        textViewWeight.setText(user.weightKg + "kg");
                        textViewAge.setText(String.valueOf(user.age));
                        textViewGoalCalorie.setText(user.dailyCalorieGoal + " Kcal");
                    }
                });
            });
        } else {
            // Handle case where user ID is invalid
            Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void logoutUser() {
        // Clear saved user ID
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(Login.PREFS_NAME, MODE_PRIVATE).edit();
        editor.remove(Login.PREF_USER_ID); // Remove the user ID
        editor.apply();

        Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}