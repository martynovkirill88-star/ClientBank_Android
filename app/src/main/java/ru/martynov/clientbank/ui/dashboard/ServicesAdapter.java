package ru.martynov.clientbank.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.models.BankService;

public class ServicesAdapter extends RecyclerView.Adapter<ServicesAdapter.ViewHolder> {
    public interface OnServiceClickListener {
        void onServiceClick(BankService service);
    }

    private final List<BankService> items = new ArrayList<>();
    private final OnServiceClickListener listener;

    public ServicesAdapter(OnServiceClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<BankService> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_button, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BankService item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvSubtitle.setText(item.subtitle != null ? item.subtitle : "");

        if ("image".equalsIgnoreCase(item.iconType) && item.iconValue != null && !item.iconValue.isEmpty()) {
            String url = item.iconValue;
            if (!url.startsWith("http")) {
                url = ApiClient.BASE_URL + url;
            }
            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .fitCenter()
                    .placeholder(R.drawable.ic_bolt)
                    .error(R.drawable.ic_bolt)
                    .into(holder.ivIcon);
        } else {
            // SF Symbol or default vector icon
            holder.ivIcon.setImageResource(R.drawable.ic_bolt);
        }

        holder.itemView.setOnClickListener(v -> listener.onServiceClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSubtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_service_icon);
            tvTitle = itemView.findViewById(R.id.tv_service_title);
            tvSubtitle = itemView.findViewById(R.id.tv_service_subtitle);
        }
    }
}
