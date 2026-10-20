package ru.martynov.clientbank.data.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class BankStatus implements Serializable {
    @SerializedName("id")
    public int id;

    @SerializedName("title")
    public String title;

    @SerializedName("text")
    public String text;

    @SerializedName("media_url")
    public String mediaUrl;

    @SerializedName("media_type")
    public String mediaType; // "photo" or "video"

    @SerializedName("duration")
    public Integer duration;

    @SerializedName("created_at")
    public String createdAt;

    public int getEffectiveDuration() {
        if (duration != null && duration > 0) return duration;
        return 5;
    }
}
