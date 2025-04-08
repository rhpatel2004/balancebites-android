package com.example.balancebites;

import android.content.Intent; // Import Intent
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private int currentUserId = -1; // Variable to hold the logged-in user's ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Get user ID passed from LoginActivity
        Intent intent = getIntent();
        currentUserId = intent.getIntExtra("USER_ID", -1);
        if (currentUserId == -1) {
            // Handle error - user ID not passed correctly, maybe go back to Login
            // For now, let's just assume it's passed.
            // finish(); // Example: finish if ID is missing
            // return;
        }


        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // Pass user ID to fragments using Bundles
            Bundle args = new Bundle();
            args.putInt("USER_ID", currentUserId);

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_add_food) {
                selectedFragment = new AddFoodFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                selectedFragment.setArguments(args); // Set arguments for the fragment
                replaceFragment(selectedFragment);
            }
            return true;
        });

        // Set the initial fragment
        if (savedInstanceState == null) {
            HomeFragment initialFragment = new HomeFragment();
            Bundle initialArgs = new Bundle();
            initialArgs.putInt("USER_ID", currentUserId);
            initialFragment.setArguments(initialArgs);
            replaceFragment(initialFragment);
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }

    // Renamed from navigateToFragment for consistency
    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void setBottomNavigationItemSelected(int itemId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(itemId);
        }
    }

    // Allow fragments to get the current user ID
    public int getCurrentUserId() {
        return currentUserId;
    }
}