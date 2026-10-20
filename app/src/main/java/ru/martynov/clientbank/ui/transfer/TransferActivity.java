package ru.martynov.clientbank.ui.transfer;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.User;

public class TransferActivity extends AppCompatActivity {
    private EditText etPhone;
    private EditText etAmount;
    private LinearLayout cardRecipientInfo;
    private TextView tvRecipientAvatar;
    private TextView tvRecipientName;
    private TextView tvError;
    private Button btnContinue;

    private User currentUser;
    private User targetRecipient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        currentUser = SessionManager.getInstance().getUser();

        etPhone = findViewById(R.id.et_recipient_phone);
        etAmount = findViewById(R.id.et_amount);
        cardRecipientInfo = findViewById(R.id.card_recipient_info);
        tvRecipientAvatar = findViewById(R.id.tv_recipient_avatar);
        tvRecipientName = findViewById(R.id.tv_recipient_name);
        tvError = findViewById(R.id.tv_transfer_error);
        btnContinue = findViewById(R.id.btn_transfer_continue);

        findViewById(R.id.btn_transfer_back).setOnClickListener(v -> finish());

        // Quick amount chips
        findViewById(R.id.chip_100).setOnClickListener(v -> etAmount.setText("100"));
        findViewById(R.id.chip_500).setOnClickListener(v -> etAmount.setText("500"));
        findViewById(R.id.chip_1000).setOnClickListener(v -> etAmount.setText("1000"));
        findViewById(R.id.chip_5000).setOnClickListener(v -> etAmount.setText("5000"));

        // Phone lookup watcher
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String clean = s.toString().replaceAll("[^0-9+]", "");
                if (clean.length() >= 10) {
                    performLookup(clean);
                } else {
                    targetRecipient = null;
                    cardRecipientInfo.setVisibility(View.GONE);
                }
            }
        });

        btnContinue.setOnClickListener(v -> validateAndShowConfirmDialog());
    }

    private void performLookup(String phone) {
        ApiClient.getInstance().lookupPhone(phone, new ApiClient.ApiCallback<User>() {
            @Override
            public void onSuccess(User recipient) {
                targetRecipient = recipient;
                cardRecipientInfo.setVisibility(View.VISIBLE);
                tvRecipientName.setText(recipient.getFullName());
                String init = (recipient.name != null && !recipient.name.isEmpty()) ?
                        String.valueOf(recipient.name.charAt(0)).toUpperCase() : "К";
                tvRecipientAvatar.setText(init);
                tvError.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                targetRecipient = null;
                cardRecipientInfo.setVisibility(View.GONE);
            }
        });
    }

    private void validateAndShowConfirmDialog() {
        tvError.setVisibility(View.GONE);
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            showError("Введите сумму перевода");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            showError("Некорректная сумма");
            return;
        }

        if (amount <= 0) {
            showError("Сумма должна быть больше 0 ₽");
            return;
        }

        if (currentUser != null && amount > currentUser.balance) {
            showError("Недостаточно средств на балансе карты");
            return;
        }

        if (targetRecipient == null) {
            showError("Получатель не найден. Проверьте номер телефона.");
            return;
        }

        if (currentUser != null && currentUser.id == targetRecipient.id) {
            showError("Нельзя перевести деньги самому себе");
            return;
        }

        // Open Confirmation Sheet Dialog
        showConfirmBottomSheet(amount);
    }

    private void showConfirmBottomSheet(double amount) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transfer_confirm, null);
        dialog.setContentView(dialogView);

        TextView tvAmount = dialogView.findViewById(R.id.tv_confirm_amount);
        TextView tvRecipient = dialogView.findViewById(R.id.tv_confirm_recipient);
        LinearLayout layoutLoading = dialogView.findViewById(R.id.layout_verification_loading);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);

        tvAmount.setText(String.format("%,.0f ₽", amount).replace(',', ' '));
        tvRecipient.setText(targetRecipient.getFullName());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            // Anti-spam & security verification step
            btnConfirm.setEnabled(false);
            btnCancel.setEnabled(false);
            layoutLoading.setVisibility(View.VISIBLE);

            if (currentUser != null && targetRecipient != null) {
                ApiClient.getInstance().executeTransfer(currentUser.id, targetRecipient.id, amount, new ApiClient.ApiCallback<Double>() {
                    @Override
                    public void onSuccess(Double newBalance) {
                        currentUser.balance = newBalance;
                        SessionManager.getInstance().saveUser(currentUser);

                        vibrateSuccess();
                        dialog.dismiss();
                        Toast.makeText(TransferActivity.this, "Перевод успешно выполнен!", Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        layoutLoading.setVisibility(View.GONE);
                        btnConfirm.setEnabled(true);
                        btnCancel.setEnabled(true);
                        Toast.makeText(TransferActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void vibrateSuccess() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(150);
            }
        }
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
