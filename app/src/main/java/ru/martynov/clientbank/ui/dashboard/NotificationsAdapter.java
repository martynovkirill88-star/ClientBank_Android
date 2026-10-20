package ru.martynov.clientbank.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.models.RoPayNotification;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
    public interface OnNotificationClickListener {
        void onNotificationClick(RoPayNotification notification, int position);
    }

    private final List<RoPayNotification> items = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationsAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<RoPayNotification> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        RoPayNotification n = items.get(pos);
        h.tvTitle.setText(n.title != null ? n.title : "Уведомление");
        h.tvMessage.setText(n.message != null ? n.message : "");
        h.tvDate.setText(n.createdAt != null ? n.createdAt : "");

        h.dotUnread.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);

        int iconRes = R.drawable.ic_bell;
        int tintColor = ContextCompat.getColor(h.itemView.getContext(), R.color.mint);

        if ("business_approved".equals(n.type)) {
            iconRes = R.drawable.ic_shield_check;
            tintColor = ContextCompat.getColor(h.itemView.getContext(), R.color.mint);
        } else if ("business_rejected".equals(n.type)) {
            iconRes = R.drawable.ic_close;
            tintColor = ContextCompat.getColor(h.itemView.getContext(), R.color.danger);
        } else if ("broadcast".equals(n.type)) {
            iconRes = R.drawable.ic_bolt;
            tintColor = ContextCompat.getColor(h.itemView.getContext(), R.color.gold);
        }

        h.ivIcon.setImageResource(iconRes);
        h.ivIcon.setColorFilter(tintColor);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(n, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivIcon;
        public final TextView tvTitle;
        public final TextView tvMessage;
        public final TextView tvDate;
        public final View dotUnread;

        public ViewHolder(@NonNull View v) {
            super(v);
            ivIcon = v.findViewById(R.id.iv_notif_icon);
            tvTitle = v.findViewById(R.id.tv_notif_title);
            tvMessage = v.findViewById(R.id.tv_notif_message);
            tvDate = v.findViewById(R.id.tv_notif_date);
            dotUnread = v.findViewById(R.id.dot_notif_unread);
        }
    }
}
