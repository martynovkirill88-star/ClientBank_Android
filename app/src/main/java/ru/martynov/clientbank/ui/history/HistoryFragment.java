package ru.martynov.clientbank.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.BankTransaction;
import ru.martynov.clientbank.data.models.User;

public class HistoryFragment extends Fragment {
    private RecyclerView rvHistory;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private ChipGroup chipGroup;

    private TransactionsAdapter adapter;
    private final List<BankTransaction> allTransactions = new ArrayList<>();
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = v.findViewById(R.id.rv_history);
        swipeRefresh = v.findViewById(R.id.swipe_refresh_history);
        tvEmpty = v.findViewById(R.id.tv_empty_history);
        chipGroup = v.findViewById(R.id.chip_group_filters);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionsAdapter();
        rvHistory.setAdapter(adapter);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_deposits)) {
                currentFilter = "deposits";
            } else if (checkedIds.contains(R.id.chip_transfers)) {
                currentFilter = "transfers";
            } else {
                currentFilter = "all";
            }
            applyFilter();
        });

        swipeRefresh.setOnRefreshListener(this::loadTransactions);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void loadTransactions() {
        User u = SessionManager.getInstance().getUser();
        if (u == null) {
            swipeRefresh.setRefreshing(false);
            return;
        }

        ApiClient.getInstance().fetchTransactions(u.id, new ApiClient.ApiCallback<List<BankTransaction>>() {
            @Override
            public void onSuccess(List<BankTransaction> result) {
                allTransactions.clear();
                if (result != null) allTransactions.addAll(result);
                applyFilter();
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onError(String error) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void applyFilter() {
        User u = SessionManager.getInstance().getUser();
        int uid = u != null ? u.id : 0;

        List<BankTransaction> filtered = new ArrayList<>();
        for (BankTransaction tx : allTransactions) {
            if ("deposits".equals(currentFilter)) {
                if (tx.isPositive(uid)) filtered.add(tx);
            } else if ("transfers".equals(currentFilter)) {
                if (!tx.isPositive(uid)) filtered.add(tx);
            } else {
                filtered.add(tx);
            }
        }

        adapter.setItems(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
