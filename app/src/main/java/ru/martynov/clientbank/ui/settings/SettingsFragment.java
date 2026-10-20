package ru.martynov.clientbank.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.materialswitch.MaterialSwitch;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.User;
import ru.martynov.clientbank.ui.auth.WelcomeActivity;
import ru.martynov.clientbank.ui.support.SupportChatActivity;

public class SettingsFragment extends Fragment {
    private TextView tvAvatar;
    private TextView tvName;
    private TextView tvPhone;
    private MaterialSwitch switchBio;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        tvAvatar = v.findViewById(R.id.tv_settings_avatar);
        tvName = v.findViewById(R.id.tv_settings_name);
        tvPhone = v.findViewById(R.id.tv_settings_phone);
        switchBio = v.findViewById(R.id.switch_biometrics);

        User u = SessionManager.getInstance().getUser();
        if (u != null) {
            tvName.setText(u.getFullName());
            tvPhone.setText(u.phone != null ? u.phone : "");
            String initial = (u.name != null && !u.name.isEmpty()) ?
                    String.valueOf(u.name.charAt(0)).toUpperCase() : "К";
            tvAvatar.setText(initial);
        }

        switchBio.setChecked(SessionManager.getInstance().isBiometricsEnabled());
        switchBio.setOnCheckedChangeListener((btn, isChecked) -> {
            SessionManager.getInstance().setBiometricsEnabled(isChecked);
            Toast.makeText(getContext(), isChecked ? "Биометрия включена" : "Биометрия отключена", Toast.LENGTH_SHORT).show();
        });

        v.findViewById(R.id.btn_copy_phone).setOnClickListener(b -> {
            if (u != null && u.phone != null && getContext() != null) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("RoPay Phone", u.phone);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Номер скопирован: " + u.phone, Toast.LENGTH_SHORT).show();
            }
        });

        v.findViewById(R.id.btn_change_pin).setOnClickListener(b -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Изменение PIN-кода")
                    .setMessage("Функция изменения PIN-кода будет доступна в следующем обновлении.")
                    .setPositiveButton("OK", null)
                    .show();
        });

        v.findViewById(R.id.btn_open_support).setOnClickListener(b -> {
            Intent intent = new Intent(getContext(), SupportChatActivity.class);
            startActivity(intent);
        });

        v.findViewById(R.id.btn_logout).setOnClickListener(b -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Выход из системы")
                    .setMessage("Вы действительно хотите выйти из своего аккаунта?")
                    .setPositiveButton("Выйти", (dialog, which) -> {
                        SessionManager.getInstance().logout();
                        Intent intent = new Intent(getContext(), WelcomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        return v;
    }
}
