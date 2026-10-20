package ru.martynov.clientbank.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import java.util.HashSet;
import java.util.Set;
import ru.martynov.clientbank.data.models.User;

public class SessionManager {
    private static final String PREF_NAME = "ropay_prefs";
    private static final String KEY_USER = "current_user";
    private static final String KEY_PIN = "user_pin";
    private static final String KEY_PHONE = "bank_user_phone";
    private static final String KEY_BIOMETRIC = "biometrics_enabled";
    private static final String KEY_VIEWED_STORIES = "viewed_stories";

    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
    }

    public static synchronized SessionManager getInstance() {
        return instance;
    }

    public void saveUser(User user) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply();
        if (user != null && user.phone != null && !user.phone.isEmpty()) {
            savePhone(user.phone);
        }
    }

    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, User.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void savePhone(String phone) {
        prefs.edit().putString(KEY_PHONE, phone).apply();
    }

    public String getSavedPhone() {
        String p = prefs.getString(KEY_PHONE, "");
        if ((p == null || p.isEmpty())) {
            User u = getUser();
            if (u != null && u.phone != null) {
                return u.phone;
            }
        }
        return p != null ? p : "";
    }

    public void savePin(String pin) {
        prefs.edit().putString(KEY_PIN, pin).apply();
    }

    public String getPin() {
        return prefs.getString(KEY_PIN, null);
    }

    public boolean hasPin() {
        String p = getPin();
        return p != null && !p.isEmpty();
    }

    public void setBiometricsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply();
    }

    public boolean isBiometricsEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC, false);
    }

    public boolean isStoryViewed(int storyId) {
        Set<String> set = prefs.getStringSet(KEY_VIEWED_STORIES, new HashSet<>());
        return set.contains(String.valueOf(storyId));
    }

    public void markStoryViewed(int storyId) {
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_VIEWED_STORIES, new HashSet<>()));
        set.add(String.valueOf(storyId));
        prefs.edit().putStringSet(KEY_VIEWED_STORIES, set).apply();
    }

    public void logout() {
        prefs.edit().remove(KEY_USER).remove(KEY_PIN).apply();
    }
}
