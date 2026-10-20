package ru.martynov.clientbank.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import ru.martynov.clientbank.MainActivity;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.User;

public class WelcomeActivity extends AppCompatActivity {
    private TextView tabLogin;
    private TextView tabRegister;
    private LinearLayout layoutLogin;
    private LinearLayout layoutRegister;

    // Login Fields
    private EditText etLoginPhone;
    private EditText etLoginPin;
    private Button btnSubmitLogin;

    // Register Fields
    private EditText etRegFirstName;
    private EditText etRegLastName;
    private EditText etRegPhone;
    private EditText etRegPin;
    private EditText etRegTelegram;
    private Button btnSubmitRegister;

    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        tabLogin = findViewById(R.id.tab_login);
        tabRegister = findViewById(R.id.tab_register);
        layoutLogin = findViewById(R.id.layout_login);
        layoutRegister = findViewById(R.id.layout_register);

        etLoginPhone = findViewById(R.id.et_login_phone);
        etLoginPin = findViewById(R.id.et_login_pin);
        btnSubmitLogin = findViewById(R.id.btn_submit_login);

        etRegFirstName = findViewById(R.id.et_reg_firstname);
        etRegLastName = findViewById(R.id.et_reg_lastname);
        etRegPhone = findViewById(R.id.et_reg_phone);
        etRegPin = findViewById(R.id.et_reg_pin);
        etRegTelegram = findViewById(R.id.et_reg_telegram);
        btnSubmitRegister = findViewById(R.id.btn_submit_register);

        tvError = findViewById(R.id.tv_auth_error);

        tabLogin.setOnClickListener(v -> switchTab(true));
        tabRegister.setOnClickListener(v -> switchTab(false));

        btnSubmitLogin.setOnClickListener(v -> performLogin());
        btnSubmitRegister.setOnClickListener(v -> performRegister());
    }

    private void switchTab(boolean isLogin) {
        tvError.setVisibility(View.GONE);
        if (isLogin) {
            tabLogin.setBackgroundResource(R.drawable.bg_button_purple);
            tabLogin.setTextColor(getColor(R.color.white));
            tabRegister.setBackgroundResource(R.color.transparent);
            tabRegister.setTextColor(getColor(R.color.text_muted));
            layoutLogin.setVisibility(View.VISIBLE);
            layoutRegister.setVisibility(View.GONE);
        } else {
            tabRegister.setBackgroundResource(R.drawable.bg_button_purple);
            tabRegister.setTextColor(getColor(R.color.white));
            tabLogin.setBackgroundResource(R.color.transparent);
            tabLogin.setTextColor(getColor(R.color.text_muted));
            layoutLogin.setVisibility(View.GONE);
            layoutRegister.setVisibility(View.VISIBLE);
        }
    }

    private void performLogin() {
        tvError.setVisibility(View.GONE);
        String phone = etLoginPhone.getText().toString().trim();
        String pin = etLoginPin.getText().toString().trim();

        if (phone.isEmpty() || pin.isEmpty()) {
            showError("Заполните номер телефона и PIN-код");
            return;
        }

        btnSubmitLogin.setEnabled(false);
        ApiClient.getInstance().loginWithPin(phone, pin, new ApiClient.ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                btnSubmitLogin.setEnabled(true);
                SessionManager.getInstance().saveUser(user);
                SessionManager.getInstance().savePin(pin);

                startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(String error) {
                btnSubmitLogin.setEnabled(true);
                showError(error);
            }
        });
    }

    private void performRegister() {
        tvError.setVisibility(View.GONE);
        String first = etRegFirstName.getText().toString().trim();
        String last = etRegLastName.getText().toString().trim();
        String phone = etRegPhone.getText().toString().trim();
        String pin = etRegPin.getText().toString().trim();
        String tg = etRegTelegram.getText().toString().trim();

        if (first.isEmpty() || last.isEmpty() || phone.isEmpty() || pin.isEmpty()) {
            showError("Пожалуйста, заполните все обязательные поля");
            return;
        }

        btnSubmitRegister.setEnabled(false);
        ApiClient.getInstance().submitApplication(first, last, phone, pin, tg, new ApiClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                btnSubmitRegister.setEnabled(true);
                new AlertDialog.Builder(WelcomeActivity.this)
                        .setTitle("Заявка принята!")
                        .setMessage("Ваша заявка на открытие счета отправлена администраторам банка RoPay. После одобрения вы сможете войти в приложение.")
                        .setPositiveButton("Понятно", (dialog, which) -> switchTab(true))
                        .show();
            }

            @Override
            public void onError(String error) {
                btnSubmitRegister.setEnabled(true);
                showError(error);
            }
        });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
