package ru.martynov.clientbank.data.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NotificationsResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("notifications")
    public List<RoPayNotification> notifications;

    @SerializedName("unread_count")
    public int unreadCount;
}
