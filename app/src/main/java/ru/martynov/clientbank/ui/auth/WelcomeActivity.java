package ru.martynov.clientbank.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import ru.martynov.clientbank.MainActivity;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.User;

public class WelcomeActivity extends AppCompatActivity {
    private CardView cardUser;
    private TextView tvName;
    private View layoutSecurity;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        cardUser = findViewById(R.id.card_welcome_user);
        tvName = findViewById(R.id.tv_welcome_name);
        layoutSecurity = findViewById(R.id.layout_welcome_security);

        String name = getIntent().getStringExtra("user_name");
        if (name == null || name.trim().isEmpty()) {
            User u = SessionManager.getInstance().getUser();
            if (u != null) {
                name = u.getFullName();
            }
        }
        if (name == null || name.trim().isEmpty()) {
            name = "Клиент RoPay";
        }

        tvName.setText(name.toUpperCase());

        // Prepare initial animation state
        cardUser.setTranslationY(-500f);
        cardUser.setAlpha(0f);
        cardUser.setScaleX(0.92f);
        cardUser.setScaleY(0.92f);

        layoutSecurity.setAlpha(0f);

        // Slide down spring animation matching iOS WelcomeView
        cardUser.animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(700)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .start();

        layoutSecurity.animate()
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // After 1.4 seconds, smoothly proceed to MainActivity
        handler.postDelayed(() -> {
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1400);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
