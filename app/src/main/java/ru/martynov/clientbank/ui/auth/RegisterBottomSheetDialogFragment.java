package ru.martynov.clientbank.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.local.SessionManager;

public class RegisterBottomSheetDialogFragment extends BottomSheetDialogFragment {
    public interface OnRegisterSuccessListener {
        void onApplicationSubmitted(String phone);
    }

    private OnRegisterSuccessListener listener;

    private EditText etName;
    private EditText etSurname;
    private EditText etPhone;
    private EditText etPin1;
    private EditText etPin2;
    private EditText etTelegram;
    private TextView tvError;
    private View btnSubmit;
    private ProgressBar pbLoading;
    private ImageView ivIcon;
    private TextView tvBtnLabel;

    public void setOnRegisterSuccessListener(OnRegisterSuccessListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_register_sheet, container, false);

        etName = v.findViewById(R.id.et_reg_name);
        etSurname = v.findViewById(R.id.et_reg_surname);
        etPhone = v.findViewById(R.id.et_reg_phone);
        etPin1 = v.findViewById(R.id.et_reg_pin1);
        etPin2 = v.findViewById(R.id.et_reg_pin2);
        etTelegram = v.findViewById(R.id.et_reg_telegram);
        tvError = v.findViewById(R.id.tv_reg_error);
        btnSubmit = v.findViewById(R.id.btn_reg_submit);
        pbLoading = v.findViewById(R.id.pb_reg_loading);
        ivIcon = v.findViewById(R.id.iv_reg_icon);
        tvBtnLabel = v.findViewById(R.id.tv_reg_btn_label);

        v.findViewById(R.id.btn_close_reg).setOnClickListener(b -> dismiss());

        btnSubmit.setOnClickListener(b -> submitForm());

        return v;
    }

    private void submitForm() {
        tvError.setVisibility(View.GONE);

        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String pin1 = etPin1.getText().toString().trim();
        String pin2 = etPin2.getText().toString().trim();
        String tg = etTelegram.getText().toString().trim();

        if (name.isEmpty()) {
            showError("Укажите ваше имя");
            return;
        }
        if (phone.length() < 10) {
            showError("Укажите корректный номер телефона");
            return;
        }
        if (pin1.length() != 4) {
            showError("ПИН-код должен содержать ровно 4 цифры");
            return;
        }
        if (!pin1.equals(pin2)) {
            showError("ПИН-коды не совпадают");
            return;
        }
        if (tg.isEmpty()) {
            showError("Укажите юзернейм в Telegram для связи");
            return;
        }

        setLoading(true);

        ApiClient.getInstance().submitApplication(name, surname, phone, pin1, tg, new ApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                setLoading(false);
                SessionManager.getInstance().savePhone(phone);

                if (getContext() != null) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Заявка принята!")
                            .setMessage("Ваша заявка на открытие счета отправлена администраторам банка RoPay. После одобрения вы сможете войти в приложение.")
                            .setPositiveButton("Понятно", (dialog, which) -> {
                                if (listener != null) {
                                    listener.onApplicationSubmitted(phone);
                                }
                                dismiss();
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    dismiss();
                }
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                showError(error);
            }
        });
    }

    private void setLoading(boolean loading) {
        btnSubmit.setEnabled(!loading);
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        ivIcon.setVisibility(loading ? View.GONE : View.VISIBLE);
        tvBtnLabel.setText(loading ? "Отправка заявки..." : "Оставить заявку");
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
