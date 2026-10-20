package ru.martynov.clientbank;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import ru.martynov.clientbank.ui.dashboard.DashboardFragment;
import ru.martynov.clientbank.ui.history.HistoryFragment;
import ru.martynov.clientbank.ui.settings.SettingsFragment;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;

    private final Fragment dashboardFragment = new DashboardFragment();
    private final Fragment historyFragment = new HistoryFragment();
    private final Fragment settingsFragment = new SettingsFragment();
    private Fragment activeFragment = dashboardFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.nav_host_container, settingsFragment, "settings").hide(settingsFragment)
                .add(R.id.nav_host_container, historyFragment, "history").hide(historyFragment)
                .add(R.id.nav_host_container, dashboardFragment, "dashboard")
                .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                switchFragment(dashboardFragment);
                return true;
            } else if (itemId == R.id.nav_history) {
                switchFragment(historyFragment);
                return true;
            } else if (itemId == R.id.nav_settings) {
                switchFragment(settingsFragment);
                return true;
            }
            return false;
        });
    }

    private void switchFragment(Fragment target) {
        if (activeFragment != target) {
            getSupportFragmentManager().beginTransaction()
                    .hide(activeFragment)
                    .show(target)
                    .commit();
            activeFragment = target;
        }
    }
}
