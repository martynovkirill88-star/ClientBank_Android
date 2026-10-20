package ru.martynov.clientbank.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;
import ru.martynov.clientbank.MainActivity;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.User;

public class PinLockActivity extends AppCompatActivity {
    private final StringBuilder enteredPin = new StringBuilder();
    private View dot1, dot2, dot3, dot4;
    private TextView tvPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        User u = SessionManager.getInstance().getUser();
        if (u == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_pin_lock);

        dot1 = findViewById(R.id.dot_1);
        dot2 = findViewById(R.id.dot_2);
        dot3 = findViewById(R.id.dot_3);
        dot4 = findViewById(R.id.dot_4);
        tvPrompt = findViewById(R.id.tv_pin_prompt);

        setupNumpad();

        findViewById(R.id.btn_pin_logout).setOnClickListener(v -> {
            SessionManager.getInstance().logout();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        });

        if (SessionManager.getInstance().isBiometricsEnabled()) {
            showBiometricPrompt();
        }
    }

    private void setupNumpad() {
        int[] numIds = {
                R.id.btn_key_0, R.id.btn_key_1, R.id.btn_key_2, R.id.btn_key_3,
                R.id.btn_key_4, R.id.btn_key_5, R.id.btn_key_6, R.id.btn_key_7,
                R.id.btn_key_8, R.id.btn_key_9
        };

        for (int id : numIds) {
            findViewById(id).setOnClickListener(v -> {
                String digit = ((TextView) v).getText().toString();
                appendDigit(digit);
            });
        }

        findViewById(R.id.btn_key_backspace).setOnClickListener(v -> removeDigit());
        findViewById(R.id.btn_key_bio).setOnClickListener(v -> showBiometricPrompt());
    }

    private void appendDigit(String digit) {
        if (enteredPin.length() < 4) {
            enteredPin.append(digit);
            updateDots();
            if (enteredPin.length() == 4) {
                checkPin();
            }
        }
    }

    private void removeDigit() {
        if (enteredPin.length() > 0) {
            enteredPin.deleteCharAt(enteredPin.length() - 1);
            updateDots();
        }
    }

    private void updateDots() {
        int len = enteredPin.length();
        dot1.setBackgroundResource(len >= 1 ? R.drawable.bg_button_mint : R.drawable.bg_circle_avatar);
        dot2.setBackgroundResource(len >= 2 ? R.drawable.bg_button_mint : R.drawable.bg_circle_avatar);
        dot3.setBackgroundResource(len >= 3 ? R.drawable.bg_button_mint : R.drawable.bg_circle_avatar);
        dot4.setBackgroundResource(len >= 4 ? R.drawable.bg_button_mint : R.drawable.bg_circle_avatar);
    }

    private void checkPin() {
        String savedPin = SessionManager.getInstance().getPin();
        User u = SessionManager.getInstance().getUser();

        boolean match = false;
        if (savedPin != null && savedPin.equals(enteredPin.toString())) {
            match = true;
        } else if (u != null && u.pinCode != null && u.pinCode.equals(enteredPin.toString())) {
            match = true;
        }

        if (match) {
            unlockSuccess();
        } else {
            enteredPin.setLength(0);
            updateDots();
            tvPrompt.setText("Неверный PIN-код, попробуйте снова");
            tvPrompt.setTextColor(getColor(R.color.danger));
        }
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                unlockSuccess();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Вход в RoPay")
                .setSubtitle("Подтвердите личность по отпечатку пальца")
                .setNegativeButtonText("Использовать PIN-код")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void unlockSuccess() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
