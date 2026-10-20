package ru.martynov.clientbank.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;
import ru.martynov.clientbank.MainActivity;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.User;

public class PinLockActivity extends AppCompatActivity {
    private final StringBuilder enteredPin = new StringBuilder();

    private TextView tvPhoneStatus;
    private LinearLayout layoutPinDots;
    private View dot1, dot2, dot3, dot4;
    private TextView tvPinError;
    private ProgressBar pbPinLoading;
    private View btnBiometricLogin;

    private boolean isLoading = false;
    private String savedPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_lock);

        tvPhoneStatus = findViewById(R.id.tv_phone_status);
        layoutPinDots = findViewById(R.id.layout_pin_dots);
        dot1 = findViewById(R.id.dot_1);
        dot2 = findViewById(R.id.dot_2);
        dot3 = findViewById(R.id.dot_3);
        dot4 = findViewById(R.id.dot_4);
        tvPinError = findViewById(R.id.tv_pin_error);
        pbPinLoading = findViewById(R.id.pb_pin_loading);
        btnBiometricLogin = findViewById(R.id.btn_biometric_login);

        refreshPhoneStatus();
        setupKeypad();

        findViewById(R.id.btn_footer_register).setOnClickListener(v -> showRegisterSheet());
        findViewById(R.id.btn_footer_switch_phone).setOnClickListener(v -> showSwitchPhoneDialog());
        btnBiometricLogin.setOnClickListener(v -> showBiometricPrompt());

        if (SessionManager.getInstance().isBiometricsEnabled() && SessionManager.getInstance().getUser() != null) {
            showBiometricPrompt();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPhoneStatus();
        enteredPin.setLength(0);
        updateDots();
    }

    private void refreshPhoneStatus() {
        savedPhone = SessionManager.getInstance().getSavedPhone();
        if (savedPhone != null && !savedPhone.trim().isEmpty()) {
            tvPhoneStatus.setText(savedPhone.trim());
            tvPhoneStatus.setTextColor(ContextCompat.getColor(this, R.color.mint));
        } else {
            tvPhoneStatus.setText("Введите ПИН-код или зарегистрируйтесь");
            tvPhoneStatus.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        }
    }

    private void setupKeypad() {
        int[] numIds = {
                R.id.btn_key_0, R.id.btn_key_1, R.id.btn_key_2, R.id.btn_key_3,
                R.id.btn_key_4, R.id.btn_key_5, R.id.btn_key_6, R.id.btn_key_7,
                R.id.btn_key_8, R.id.btn_key_9
        };

        for (int id : numIds) {
            findViewById(id).setOnClickListener(v -> {
                if (isLoading) return;
                hapticFeedback();
                String digit = ((TextView) v).getText().toString();
                appendDigit(digit);
            });
        }

        findViewById(R.id.btn_key_clear).setOnClickListener(v -> {
            if (isLoading) return;
            hapticFeedback();
            enteredPin.setLength(0);
            updateDots();
            tvPinError.setVisibility(View.INVISIBLE);
        });

        findViewById(R.id.btn_key_backspace).setOnClickListener(v -> {
            if (isLoading) return;
            hapticFeedback();
            if (enteredPin.length() > 0) {
                enteredPin.deleteCharAt(enteredPin.length() - 1);
                updateDots();
                tvPinError.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void appendDigit(String digit) {
        if (enteredPin.length() < 4) {
            enteredPin.append(digit);
            updateDots();
            if (enteredPin.length() == 4) {
                verifyPin();
            }
        }
    }

    private void updateDots() {
        int len = enteredPin.length();
        dot1.setBackgroundResource(len >= 1 ? R.drawable.bg_button_mint : R.drawable.bg_circle_avatar);
        dot2.setBackgroundResource(len >= 2 ? R.drawable.bg_button_mint : R.drawable.bg_circle_avatar);
        dot3.setBackgroundResource(len >= 3 ? R.drawable.bg_button_mint : R.drawable.bg_circle_avatar);
        dot4.setBackgroundResource(len >= 4 ? R.drawable.bg_button_mint : R.drawable.bg_circle_avatar);

        dot1.setScaleX(len >= 1 ? 1.15f : 1.0f);
        dot1.setScaleY(len >= 1 ? 1.15f : 1.0f);
        dot2.setScaleX(len >= 2 ? 1.15f : 1.0f);
        dot2.setScaleY(len >= 2 ? 1.15f : 1.0f);
        dot3.setScaleX(len >= 3 ? 1.15f : 1.0f);
        dot3.setScaleY(len >= 3 ? 1.15f : 1.0f);
        dot4.setScaleX(len >= 4 ? 1.15f : 1.0f);
        dot4.setScaleY(len >= 4 ? 1.15f : 1.0f);
    }

    private void verifyPin() {
        if (savedPhone == null || savedPhone.trim().isEmpty()) {
            shakePin();
            showError("Сначала зарегистрируйтесь или укажите номер");
            showSwitchPhoneDialog();
            return;
        }

        final String pin = enteredPin.toString();

        // 1. Instant local offline unlock if cached PIN and User match
        String cachedPin = SessionManager.getInstance().getPin();
        User localUser = SessionManager.getInstance().getUser();
        if (cachedPin != null && cachedPin.equals(pin) && localUser != null) {
            unlockSuccess(localUser);

            // Sync fresh profile data from server in background
            ApiClient.getInstance().loginWithPin(savedPhone.trim(), pin, new ApiClient.ApiCallback<User>() {
                @Override
                public void onSuccess(User freshUser) {
                    SessionManager.getInstance().saveUser(freshUser);
                }

                @Override
                public void onError(String error) {
                    // Ignore background sync errors
                }
            });
            return;
        }

        // 2. Online verification with RoPay Server
        setLoading(true);
        ApiClient.getInstance().loginWithPin(savedPhone.trim(), pin, new ApiClient.ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                SessionManager.getInstance().saveUser(user);
                SessionManager.getInstance().savePin(pin);
                unlockSuccess(user);
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                shakePin();
                showError(error);
                enteredPin.setLength(0);
                updateDots();
            }
        });
    }

    private void unlockSuccess(User user) {
        Intent intent = new Intent(PinLockActivity.this, WelcomeActivity.class);
        if (user != null) {
            intent.putExtra("user_name", user.getFullName());
        }
        startActivity(intent);
        finish();
    }

    private void shakePin() {
        Animation shake = new TranslateAnimation(0, 20, 0, 0);
        shake.setInterpolator(new CycleInterpolator(4));
        shake.setDuration(450);
        layoutPinDots.startAnimation(shake);
    }

    private void showError(String msg) {
        tvPinError.setText(msg);
        tvPinError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        this.isLoading = loading;
        pbPinLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        layoutPinDots.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
    }

    private void showRegisterSheet() {
        RegisterBottomSheetDialogFragment sheet = new RegisterBottomSheetDialogFragment();
        sheet.setOnRegisterSuccessListener(phone -> {
            refreshPhoneStatus();
            enteredPin.setLength(0);
            updateDots();
        });
        sheet.show(getSupportFragmentManager(), "RegisterSheet");
    }

    private void showSwitchPhoneDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_transfer_confirm, null);
        EditText input = new EditText(this);
        input.setHint("+7 999 123-45-67");
        input.setText(savedPhone);
        input.setTextColor(ContextCompat.getColor(this, R.color.white));
        input.setHintTextColor(ContextCompat.getColor(this, R.color.text_muted));
        input.setBackgroundResource(R.drawable.bg_input_field);
        input.setPadding(32, 28, 32, 28);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(48, 24, 48, 24);
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Сменить номер телефона")
                .setMessage("Введите номер телефона, привязанный к вашему счету RoPay:")
                .setView(container)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newPhone = input.getText().toString().trim();
                    if (!newPhone.isEmpty()) {
                        SessionManager.getInstance().savePhone(newPhone);
                        refreshPhoneStatus();
                        enteredPin.setLength(0);
                        updateDots();
                        tvPinError.setVisibility(View.INVISIBLE);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                User localUser = SessionManager.getInstance().getUser();
                if (localUser != null) {
                    unlockSuccess(localUser);
                } else if (savedPhone != null && !savedPhone.isEmpty() && SessionManager.getInstance().hasPin()) {
                    verifyPin();
                } else {
                    Toast.makeText(PinLockActivity.this, "Пожалуйста, войдите по ПИН-коду первый раз", Toast.LENGTH_SHORT).show();
                }
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

    private void hapticFeedback() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(20);
            }
        }
    }
}
