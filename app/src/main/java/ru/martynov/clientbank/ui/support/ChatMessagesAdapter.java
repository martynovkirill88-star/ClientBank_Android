package ru.martynov.clientbank.ui.support;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.models.SupportMessage;

public class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder> {
    private final List<SupportMessage> items = new ArrayList<>();

    public void setItems(List<SupportMessage> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SupportMessage msg = items.get(position);
        if (msg.isUser()) {
            holder.layoutIncoming.setVisibility(View.GONE);
            holder.layoutOutgoing.setVisibility(View.VISIBLE);
            holder.tvOutgoingText.setText(msg.text);
        } else {
            holder.layoutOutgoing.setVisibility(View.GONE);
            holder.layoutIncoming.setVisibility(View.VISIBLE);
            holder.tvIncomingText.setText(msg.text);
            holder.tvSenderName.setText(msg.senderName != null ? msg.senderName : "Бот-помощник");
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutIncoming;
        LinearLayout layoutOutgoing;
        TextView tvSenderName;
        TextView tvIncomingText;
        TextView tvOutgoingText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutIncoming = itemView.findViewById(R.id.layout_incoming);
            layoutOutgoing = itemView.findViewById(R.id.layout_outgoing);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            tvIncomingText = itemView.findViewById(R.id.tv_incoming_text);
            tvOutgoingText = itemView.findViewById(R.id.tv_outgoing_text);
        }
    }
}
