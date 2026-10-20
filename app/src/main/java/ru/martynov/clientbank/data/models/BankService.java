package ru.martynov.clientbank.data.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class BankService implements Serializable {
    @SerializedName("id")
    public int id;

    @SerializedName("title")
    public String title;

    @SerializedName("subtitle")
    public String subtitle;

    @SerializedName("icon_type")
    public String iconType; // "symbol" or "image"

    @SerializedName("icon_value")
    public String iconValue;

    @SerializedName("target_url")
    public String targetUrl;

    @SerializedName("is_active")
    public Integer isActive;

    @SerializedName("sort_order")
    public Integer sortOrder;
}
