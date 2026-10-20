package ru.martynov.clientbank.ui.dashboard;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
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
        holder.tvTitle.setText(item.title != null && !item.title.isEmpty() ? item.title : "RoPay");

        boolean viewed = SessionManager.getInstance().isStoryViewed(item.id);
        holder.flContainer.setBackgroundResource(viewed ? R.drawable.bg_status_square_viewed : R.drawable.bg_status_square_unviewed);
        holder.itemView.setAlpha(viewed ? 0.75f : 1.0f);

        String url = item.mediaUrl != null ? item.mediaUrl : "";
        if (!url.isEmpty() && !url.startsWith("http")) {
            url = ApiClient.BASE_URL + url;
        }

        holder.flFallback.setVisibility(View.GONE);
        holder.ivThumb.setVisibility(View.VISIBLE);

        if (!url.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .centerCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            holder.flFallback.setVisibility(View.VISIBLE);
                            holder.ivThumb.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            holder.flFallback.setVisibility(View.GONE);
                            holder.ivThumb.setVisibility(View.VISIBLE);
                            return false;
                        }
                    })
                    .into(holder.ivThumb);
        } else {
            holder.flFallback.setVisibility(View.VISIBLE);
            holder.ivThumb.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onStoryClick(item, position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout flContainer;
        ImageView ivThumb;
        FrameLayout flFallback;
        ImageView ivFallbackIcon;
        TextView tvTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            flContainer = itemView.findViewById(R.id.fl_status_container);
            ivThumb = itemView.findViewById(R.id.iv_story_thumb);
            flFallback = itemView.findViewById(R.id.fl_story_fallback);
            ivFallbackIcon = itemView.findViewById(R.id.iv_fallback_icon);
            tvTitle = itemView.findViewById(R.id.tv_story_title);
        }
    }
}
