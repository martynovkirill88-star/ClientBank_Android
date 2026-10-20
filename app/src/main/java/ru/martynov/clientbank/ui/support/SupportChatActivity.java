package ru.martynov.clientbank.ui.support;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.SupportChat;
import ru.martynov.clientbank.data.models.SupportMessage;
import ru.martynov.clientbank.data.models.User;

public class SupportChatActivity extends AppCompatActivity {
    private RecyclerView rvMessages;
    private EditText etInput;
    private ImageView btnSend;

    private ChatMessagesAdapter adapter;
    private SupportChat currentChat;
    private User currentUser;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentChat != null) {
                pollMessages();
            }
            pollHandler.postDelayed(this, 2500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_chat);

        currentUser = SessionManager.getInstance().getUser();

        rvMessages = findViewById(R.id.rv_chat_messages);
        etInput = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.btn_chat_send);

        findViewById(R.id.btn_support_back).setOnClickListener(v -> finish());

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);

        adapter = new ChatMessagesAdapter();
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());

        initChat();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.post(pollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void initChat() {
        int uid = currentUser != null ? currentUser.id : 0;
        String name = currentUser != null ? currentUser.getFullName() : "Клиент";
        String phone = currentUser != null && currentUser.phone != null ? currentUser.phone : "";

        ApiClient.getInstance().initSupportChat(uid, name, phone, new ApiClient.ApiCallback<SupportChat>() {
            @Override
            public void onSuccess(SupportChat chat) {
                currentChat = chat;
                pollMessages();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SupportChatActivity.this, "Ошибка подключения: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pollMessages() {
        if (currentChat == null) return;
        ApiClient.getInstance().fetchSupportMessages(currentChat.id, new ApiClient.ApiCallback<List<SupportMessage>>() {
            @Override
            public void onSuccess(List<SupportMessage> messages) {
                adapter.setItems(messages);
                if (messages != null && !messages.isEmpty()) {
                    rvMessages.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() || currentChat == null) return;

        etInput.setText("");
        String senderName = currentUser != null ? currentUser.name : "Клиент";

        ApiClient.getInstance().sendSupportMessage(currentChat.id, "user", senderName, text, new ApiClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                pollMessages();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SupportChatActivity.this, "Ошибка отправки", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
