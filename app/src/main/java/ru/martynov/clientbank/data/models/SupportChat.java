package ru.martynov.clientbank.data.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class SupportChat implements Serializable {
    @SerializedName("id")
    public int id;

    @SerializedName("user_id")
    public int userId;

    @SerializedName("status")
    public String status; // "bot", "waiting_operator", "operator_active"
}
