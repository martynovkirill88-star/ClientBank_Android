package ru.martynov.clientbank.ui.dashboard;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.List;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.NotificationsResponse;
import ru.martynov.clientbank.data.models.RoPayNotification;
import ru.martynov.clientbank.data.models.User;

public class NotificationsBottomSheetDialogFragment extends BottomSheetDialogFragment {
    public interface OnNotificationsDismissListener {
        void onDismissed();
    }

    private OnNotificationsDismissListener dismissListener;

    private TextView tvSubtitle;
    private TextView btnMarkAllRead;
    private ProgressBar pbLoading;
    private View layoutEmpty;
    private RecyclerView rvNotifications;

    private NotificationsAdapter adapter;
    private final List<RoPayNotification> notificationList = new ArrayList<>();
    private User currentUser;

    public void setOnNotificationsDismissListener(OnNotificationsDismissListener listener) {
        this.dismissListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_notifications, container, false);

        tvSubtitle = v.findViewById(R.id.tv_notif_subtitle);
        btnMarkAllRead = v.findViewById(R.id.btn_notif_mark_all_read);
        pbLoading = v.findViewById(R.id.pb_notif_loading);
        layoutEmpty = v.findViewById(R.id.layout_notif_empty);
        rvNotifications = v.findViewById(R.id.rv_notifications);

        currentUser = SessionManager.getInstance().getUser();

        v.findViewById(R.id.btn_notif_close).setOnClickListener(b -> dismiss());

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationsAdapter((notif, pos) -> markNotificationAsRead(notif, pos));
        rvNotifications.setAdapter(adapter);

        btnMarkAllRead.setOnClickListener(b -> markAllNotificationsAsRead());

        loadNotifications();

        return v;
    }

    private void loadNotifications() {
        if (currentUser == null) {
            showEmpty();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.GONE);

        ApiClient.getInstance().fetchNotifications(currentUser.id, new ApiClient.ApiCallback<NotificationsResponse>() {
            @Override
            public void onSuccess(NotificationsResponse res) {
                pbLoading.setVisibility(View.GONE);
                if (res != null && res.notifications != null && !res.notifications.isEmpty()) {
                    notificationList.clear();
                    notificationList.addAll(res.notifications);
                    adapter.setItems(notificationList);

                    rvNotifications.setVisibility(View.VISIBLE);
                    layoutEmpty.setVisibility(View.GONE);

                    updateHeader(res.unreadCount);
                } else {
                    showEmpty();
                }
            }

            @Override
            public void onError(String error) {
                pbLoading.setVisibility(View.GONE);
                showEmpty();
            }
        });
    }

    private void updateHeader(int unreadCount) {
        if (unreadCount > 0) {
            tvSubtitle.setText(unreadCount + " новых сообщений");
            btnMarkAllRead.setVisibility(View.VISIBLE);
        } else {
            tvSubtitle.setText("Все прочитано");
            btnMarkAllRead.setVisibility(View.GONE);
        }
    }

    private void markNotificationAsRead(RoPayNotification notif, int pos) {
        if (currentUser == null || notif.isRead()) return;

        notif.isRead = 1;
        adapter.notifyItemChanged(pos);

        ApiClient.getInstance().markNotificationsRead(currentUser.id, notif.id, null);

        int remainingUnread = 0;
        for (RoPayNotification n : notificationList) {
            if (!n.isRead()) remainingUnread++;
        }
        updateHeader(remainingUnread);
    }

    private void markAllNotificationsAsRead() {
        if (currentUser == null) return;

        for (RoPayNotification n : notificationList) {
            n.isRead = 1;
        }
        adapter.notifyDataSetChanged();
        updateHeader(0);

        ApiClient.getInstance().markNotificationsRead(currentUser.id, null, null);
    }

    private void showEmpty() {
        rvNotifications.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        updateHeader(0);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.onDismissed();
        }
    }
}
