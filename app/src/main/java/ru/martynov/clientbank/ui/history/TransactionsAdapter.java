package ru.martynov.clientbank.ui.history;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.BankTransaction;
import ru.martynov.clientbank.data.models.User;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {
    private final List<BankTransaction> items = new ArrayList<>();
    private final int currentUserId;

    public TransactionsAdapter() {
        User u = SessionManager.getInstance().getUser();
        this.currentUserId = u != null ? u.id : 0;
    }

    public void setItems(List<BankTransaction> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BankTransaction item = items.get(position);
        boolean isPositive = item.isPositive(currentUserId);

        holder.tvIcon.setText(isPositive ? "+" : "−");
        holder.tvIcon.setTextColor(isPositive ? Color.parseColor("#00E599") : Color.parseColor("#FF4D4D"));

        String title;
        if ("deposit".equalsIgnoreCase(item.type)) {
            title = "Пополнение счета";
        } else if (isPositive) {
            title = "Входящий перевод от " + (item.senderName != null ? item.senderName : "клиента");
        } else {
            title = "Перевод для " + (item.recipientName != null ? item.recipientName : "клиента");
        }

        holder.tvTitle.setText(title);
        holder.tvDate.setText(item.timestamp != null ? item.timestamp : "");
        holder.tvAmount.setText(item.getFormattedAmount(currentUserId));
        holder.tvAmount.setTextColor(isPositive ? Color.parseColor("#00E599") : Color.parseColor("#FFFFFF"));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon;
        TextView tvTitle;
        TextView tvDate;
        TextView tvAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_tx_icon);
            tvTitle = itemView.findViewById(R.id.tv_tx_title);
            tvDate = itemView.findViewById(R.id.tv_tx_date);
            tvAmount = itemView.findViewById(R.id.tv_tx_amount);
        }
    }
}
