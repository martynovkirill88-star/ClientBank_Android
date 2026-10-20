package ru.martynov.clientbank.data.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class RoPayNotification implements Serializable {
    @SerializedName("id")
    public int id;

    @SerializedName("user_id")
    public int userId;

    @SerializedName("title")
    public String title;

    @SerializedName("message")
    public String message;

    @SerializedName("type")
    public String type; // "business_approved", "business_rejected", "broadcast", "general"

    @SerializedName("is_read")
    public int isRead;

    @SerializedName("created_at")
    public String createdAt;

    public boolean isRead() {
        return isRead == 1;
    }
}
