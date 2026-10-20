package ru.martynov.clientbank;

import android.app.Application;
import ru.martynov.clientbank.data.local.SessionManager;

public class RoPayApplication extends Application {
    private static RoPayApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SessionManager.init(this);
    }

    public static RoPayApplication getInstance() {
        return instance;
    }
}
