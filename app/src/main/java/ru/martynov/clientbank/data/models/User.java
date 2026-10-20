package ru.martynov.clientbank.data.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class User implements Serializable {
    @SerializedName("id")
    public int id;

    @SerializedName("name")
    public String name;

    @SerializedName("surname")
    public String surname;

    @SerializedName("phone")
    public String phone;

    @SerializedName("balance")
    public double balance;

    @SerializedName("pin_code")
    public String pinCode;

    @SerializedName("photo_filename")
    public String photoFilename;

    @SerializedName("has_face")
    public Boolean hasFace;

    public String getFullName() {
        String n = name != null ? name : "";
        String s = surname != null ? surname : "";
        return (n + " " + s).trim();
    }

    public String getFormattedBalance() {
        return String.format("%,.0f ₽", balance).replace(',', ' ');
    }
}
