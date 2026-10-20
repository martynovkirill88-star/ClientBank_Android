package ru.martynov.clientbank.data.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class BankTransaction implements Serializable {
    @SerializedName("id")
    public int id;

    @SerializedName("sender_id")
    public int senderId;

    @SerializedName("recipient_id")
    public int recipientId;

    @SerializedName("sender_name")
    public String senderName;

    @SerializedName("recipient_name")
    public String recipientName;

    @SerializedName("amount")
    public double amount;

    @SerializedName("type")
    public String type; // "deposit", "transfer", "withdrawal"

    @SerializedName("description")
    public String description;

    @SerializedName("timestamp")
    public String timestamp;

    public boolean isPositive(int currentUserId) {
        if ("deposit".equalsIgnoreCase(type)) return true;
        return recipientId == currentUserId;
    }

    public String getFormattedAmount(int currentUserId) {
        boolean positive = isPositive(currentUserId);
        String prefix = positive ? "+ " : "- ";
        return prefix + String.format("%,.0f ₽", amount).replace(',', ' ');
    }
}
