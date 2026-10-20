package ru.martynov.clientbank.data.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class SupportMessage implements Serializable {
    @SerializedName("id")
    public int id;

    @SerializedName("chat_id")
    public int chatId;

    @SerializedName("sender")
    public String sender; // "user", "bot", "operator"

    @SerializedName("sender_name")
    public String senderName;

    @SerializedName("text")
    public String text;

    @SerializedName("created_at")
    public String createdAt;

    public boolean isUser() {
        return "user".equalsIgnoreCase(sender);
    }
}
