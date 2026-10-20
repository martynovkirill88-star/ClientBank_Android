package ru.martynov.clientbank.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.BankService;
import ru.martynov.clientbank.data.models.BankStatus;
import ru.martynov.clientbank.data.models.User;
import ru.martynov.clientbank.ui.browser.RoPayInAppBrowserActivity;
import ru.martynov.clientbank.ui.story.StoryViewerActivity;
import ru.martynov.clientbank.ui.transfer.TransferActivity;

public class DashboardFragment extends Fragment {
    private TextView tvAvatar;
    private TextView tvUserName;
    private TextView tvBalance;
    private ImageView btnHideBalance;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvStories;
    private RecyclerView rvServices;

    private StoriesAdapter storiesAdapter;
    private ServicesAdapter servicesAdapter;

    private boolean isBalanceHidden = false;
    private User currentUser;
    private final List<BankStatus> loadedStatuses = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvAvatar = v.findViewById(R.id.tv_avatar);
        tvUserName = v.findViewById(R.id.tv_user_name);
        tvBalance = v.findViewById(R.id.tv_balance);
        btnHideBalance = v.findViewById(R.id.btn_hide_balance);
        swipeRefresh = v.findViewById(R.id.swipe_refresh);
        rvStories = v.findViewById(R.id.rv_stories);
        rvServices = v.findViewById(R.id.rv_services);

        currentUser = SessionManager.getInstance().getUser();
        updateUserUI();

        // Setup Stories List
        rvStories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        storiesAdapter = new StoriesAdapter((status, pos) -> {
            Intent intent = new Intent(getContext(), StoryViewerActivity.class);
            intent.putExtra("selected_index", pos);
            intent.putExtra("statuses_list", new ArrayList<>(loadedStatuses));
            startActivity(intent);
        });
        rvStories.setAdapter(storiesAdapter);

        // Setup Services List
        rvServices.setLayoutManager(new LinearLayoutManager(getContext()));
        servicesAdapter = new ServicesAdapter(service -> {
            Intent intent = new Intent(getContext(), RoPayInAppBrowserActivity.class);
            intent.putExtra("target_url", service.targetUrl);
            intent.putExtra("title", service.title);
            intent.putExtra("user_id", currentUser != null ? currentUser.id : 0);
            startActivity(intent);
        });
        rvServices.setAdapter(servicesAdapter);

        // Button Actions
        btnHideBalance.setOnClickListener(b -> toggleBalanceVisibility());

        v.findViewById(R.id.btn_action_transfer).setOnClickListener(b -> {
            Intent intent = new Intent(getContext(), TransferActivity.class);
            startActivity(intent);
        });

        v.findViewById(R.id.btn_action_atm).setOnClickListener(b -> showBleUpcomingDialog());
        v.findViewById(R.id.banner_bluetooth).setOnClickListener(b -> showBleUpcomingDialog());

        swipeRefresh.setOnRefreshListener(this::refreshData);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    private void updateUserUI() {
        if (currentUser == null) return;
        tvUserName.setText(currentUser.name != null ? currentUser.name : "Клиент");
        String initial = (currentUser.name != null && !currentUser.name.isEmpty()) ?
                String.valueOf(currentUser.name.charAt(0)).toUpperCase() : "К";
        tvAvatar.setText(initial);

        if (isBalanceHidden) {
            tvBalance.setText("•••••• ₽");
        } else {
            tvBalance.setText(currentUser.getFormattedBalance());
        }
    }

    private void toggleBalanceVisibility() {
        isBalanceHidden = !isBalanceHidden;
        updateUserUI();
    }

    private void showBleUpcomingDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Скоро будет доступно")
                .setMessage("Бесконтактный вход по Bluetooth скоро появится в приложении банка RoPay.")
                .setPositiveButton("Понятно", null)
                .show();
    }

    private void refreshData() {
        if (currentUser != null) {
            ApiClient.getInstance().fetchProfile(currentUser.id, new ApiClient.ApiCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    currentUser = user;
                    SessionManager.getInstance().saveUser(user);
                    updateUserUI();
                    swipeRefresh.setRefreshing(false);
                }

                @Override
                public void onError(String error) {
                    swipeRefresh.setRefreshing(false);
                }
            });
        } else {
            swipeRefresh.setRefreshing(false);
        }

        // Load Statuses
        ApiClient.getInstance().fetchStatuses(new ApiClient.ApiCallback<List<BankStatus>>() {
            @Override
            public void onSuccess(List<BankStatus> result) {
                loadedStatuses.clear();
                if (result != null) loadedStatuses.addAll(result);
                storiesAdapter.setItems(result);
            }

            @Override
            public void onError(String error) { }
        });

        // Load Dynamic Tele2 Services
        ApiClient.getInstance().fetchServices(new ApiClient.ApiCallback<List<BankService>>() {
            @Override
            public void onSuccess(List<BankService> result) {
                servicesAdapter.setItems(result);
            }

            @Override
            public void onError(String error) { }
        });
    }
}
