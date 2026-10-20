package ru.martynov.clientbank.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.BankStatus;

public class StoriesAdapter extends RecyclerView.Adapter<StoriesAdapter.ViewHolder> {
    public interface OnStoryClickListener {
        void onStoryClick(BankStatus status, int position);
    }

    private final List<BankStatus> items = new ArrayList<>();
    private final OnStoryClickListener listener;

    public StoriesAdapter(OnStoryClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<BankStatus> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BankStatus item = items.get(position);
        holder.tvTitle.setText(item.title != null ? item.title : "История");

        boolean viewed = SessionManager.getInstance().isStoryViewed(item.id);
        holder.flRing.setBackgroundResource(viewed ? R.drawable.bg_story_ring_viewed : R.drawable.bg_story_ring_unviewed);

        String url = item.mediaUrl != null ? item.mediaUrl : "";
        if (!url.startsWith("http")) {
            url = ApiClient.BASE_URL + url;
        }

        Glide.with(holder.itemView.getContext())
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ropay_logo)
                .error(R.drawable.ropay_logo)
                .into(holder.ivThumb);

        holder.itemView.setOnClickListener(v -> listener.onStoryClick(item, position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout flRing;
        ImageView ivThumb;
        TextView tvTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            flRing = itemView.findViewById(R.id.fl_story_ring);
            ivThumb = itemView.findViewById(R.id.iv_story_thumb);
            tvTitle = itemView.findViewById(R.id.tv_story_title);
        }
    }
}
