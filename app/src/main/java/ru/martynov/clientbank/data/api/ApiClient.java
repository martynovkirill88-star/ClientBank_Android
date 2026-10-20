package ru.martynov.clientbank.data.api;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import ru.martynov.clientbank.data.models.BankService;
import ru.martynov.clientbank.data.models.BankStatus;
import ru.martynov.clientbank.data.models.BankTransaction;
import ru.martynov.clientbank.data.models.NotificationsResponse;
import ru.martynov.clientbank.data.models.SupportChat;
import ru.martynov.clientbank.data.models.SupportMessage;
import ru.martynov.clientbank.data.models.User;

public class ApiClient {
    public static final String BASE_URL = "https://app.ropay.site";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private static ApiClient instance;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Handler mainHandler;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private ApiClient() {
        this.httpClient = buildOkHttpClient();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private OkHttpClient buildOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);

        try {
            // Trust manager that handles modern TLS on older Android (e.g. Android 5.1 Lollipop / API 21-22)
            final X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            };

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            final SSLSocketFactory sslSocketFactory = new Tls12SocketFactory(sslContext.getSocketFactory());

            builder.sslSocketFactory(sslSocketFactory, trustManager);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return builder.build();
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    private <T> void deliverSuccess(ApiCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void deliverError(ApiCallback<T> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }

    // ── Login with PIN ──────────────────────────────────────────────────────────
    public void loginWithPin(String phone, String pin, ApiCallback<User> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("pin", pin);

        Request req = new Request.Builder()
                .url(BASE_URL + "/api/client/login_pin")
                .post(RequestBody.create(gson.toJson(body), JSON_MEDIA))
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, "Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverError(callback, "Неверный телефон или PIN-код");
                    return;
                }
                try {
                    String respStr = response.body().string();
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    if (obj.has("success") && !obj.get("success").getAsBoolean()) {
                        String msg = obj.has("message") ? obj.get("message").getAsString() : "Неверный PIN-код";
                        deliverError(callback, msg);
                        return;
                    }
                    if (obj.has("user") && !obj.get("user").isJsonNull()) {
                        User u = gson.fromJson(obj.get("user"), User.class);
                        deliverSuccess(callback, u);
                    } else {
                        deliverError(callback, "Ошибка обработки ответа сервера");
                    }
                } catch (Exception e) {
                    deliverError(callback, "Ошибка парсинга профиля");
                }
            }
        });
    }

    // ── Register Application ────────────────────────────────────────────────────
    public void submitApplication(String firstName, String lastName, String phone, String pin, String telegram, ApiCallback<String> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("name", firstName);
        body.put("surname", lastName);
        body.put("phone", phone);
        body.put("pin", pin);
        body.put("telegram", telegram);

        Request req = new Request.Builder()
                .url(BASE_URL + "/api/client/application")
                .post(RequestBody.create(gson.toJson(body), JSON_MEDIA))
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, "Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String respStr = response.body().string();
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    if (obj != null && obj.has("success") && obj.get("success").getAsBoolean()) {
                        String msg = obj.has("message") ? obj.get("message").getAsString() : "Заявка успешно отправлена";
                        deliverSuccess(callback, msg);
                    } else {
                        String msg = (obj != null && obj.has("message")) ? obj.get("message").getAsString() : "Не удалось отправить заявку";
                        deliverError(callback, msg);
                    }
                } catch (Exception e) {
                    deliverError(callback, "Ошибка обработки заявки");
                }
            }
        });
    }

    // ── Direct Register ─────────────────────────────────────────────────────────
    public void register(String firstName, String lastName, String phone, String pin, ApiCallback<User> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("name", firstName);
        body.put("surname", lastName);
        body.put("phone", phone);
        body.put("pin", pin);

        Request req = new Request.Builder()
                .url(BASE_URL + "/api/client/register")
                .post(RequestBody.create(gson.toJson(body), JSON_MEDIA))
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, "Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String respStr = response.body().string();
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    if (obj != null && obj.has("success") && obj.get("success").getAsBoolean()) {
                        User u = gson.fromJson(obj.get("user"), User.class);
                        deliverSuccess(callback, u);
                    } else {
                        String msg = (obj != null && obj.has("message")) ? obj.get("message").getAsString() : "Ошибка регистрации";
                        deliverError(callback, msg);
                    }
                } catch (Exception e) {
                    deliverError(callback, "Ошибка парсинга ответа регистрации");
                }
            }
        });
    }

    // ── Notifications API ───────────────────────────────────────────────────────
    public void fetchNotifications(int userId, ApiCallback<NotificationsResponse> callback) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/client/notifications/" + userId)
                .get()
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, "Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverError(callback, "Не удалось загрузить уведомления");
                    return;
                }
                try {
                    String respStr = response.body().string();
                    NotificationsResponse resp = gson.fromJson(respStr, NotificationsResponse.class);
                    deliverSuccess(callback, resp);
                } catch (Exception e) {
                    deliverError(callback, "Ошибка парсинга уведомлений");
                }
            }
        });
    }

    public void markNotificationsRead(int userId, Integer notificationId, ApiCallback<Boolean> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        if (notificationId != null) {
            body.put("notification_id", notificationId);
        }

        Request req = new Request.Builder()
                .url(BASE_URL + "/api/client/notifications/mark-read")
                .post(RequestBody.create(gson.toJson(body), JSON_MEDIA))
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) deliverError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (callback != null) deliverSuccess(callback, response.isSuccessful());
            }
        });
    }

    // ── Fetch Profile ───────────────────────────────────────────────────────────
    public void fetchProfile(int userId, ApiCallback<User> callback) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/client/profile/" + userId)
                .get()
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverError(callback, "Не удалось обновить профиль");
                    return;
                }
                try {
                    String respStr = response.body() != null ? response.body().string() : "";
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    User u = null;
                    if (obj != null && obj.has("user") && !obj.get("user").isJsonNull()) {
                        u = gson.fromJson(obj.get("user"), User.class);
                    } else if (obj != null) {
                        u = gson.fromJson(obj, User.class);
                    }
                    if (u != null) {
                        deliverSuccess(callback, u);
                    } else {
                        deliverError(callback, "Ошибка разбора профиля");
                    }
                } catch (Exception e) {
                    deliverError(callback, e.getMessage());
                }
            }
        });
    }

    // ── Fetch User By Phone ──────────────────────────────────────────────────────
    public void fetchUserByPhone(String phone, ApiCallback<User> callback) {
        String cleanPhone = (phone != null) ? phone.trim().replace(" ", "").replace("-", "") : "";
        try {
            cleanPhone = java.net.URLEncoder.encode(cleanPhone, "UTF-8");
        } catch (Exception ignored) {}

        Request req = new Request.Builder()
                .url(BASE_URL + "/api/client/by_phone?phone=" + cleanPhone)
                .get()
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, "Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverError(callback, "Клиент не найден");
                    return;
                }
                try {
                    String respStr = response.body() != null ? response.body().string() : "";
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    User u = null;
                    if (obj != null && obj.has("user") && !obj.get("user").isJsonNull()) {
                        u = gson.fromJson(obj.get("user"), User.class);
                    } else if (obj != null) {
                        u = gson.fromJson(obj, User.class);
                    }
                    if (u != null) {
                        deliverSuccess(callback, u);
                    } else {
                        deliverError(callback, "Ошибка парсинга профиля");
                    }
                } catch (Exception e) {
                    deliverError(callback, "Ошибка обработки ответа: " + e.getMessage());
                }
            }
        });
    }

    // ── Fetch Transactions ──────────────────────────────────────────────────────
    public void fetchTransactions(int userId, ApiCallback<List<BankTransaction>> callback) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/client/transactions/" + userId)
                .get()
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverError(callback, "Не удалось загрузить историю");
                    return;
                }
                try {
                    String respStr = response.body().string();
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    Type listType = new TypeToken<List<BankTransaction>>(){}.getType();
                    List<BankTransaction> list = gson.fromJson(obj.get("transactions"), listType);
                    deliverSuccess(callback, list);
                } catch (Exception e) {
                    deliverError(callback, e.getMessage());
                }
            }
        });
    }

    // ── Fetch Stories / Statuses ────────────────────────────────────────────────
    public void fetchStatuses(ApiCallback<List<BankStatus>> callback) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/statuses?platform=android")
                .get()
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverError(callback, "Не удалось загрузить статусы");
                    return;
                }
                try {
                    String respStr = response.body().string();
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    Type listType = new TypeToken<List<BankStatus>>(){}.getType();
                    List<BankStatus> list = gson.fromJson(obj.get("statuses"), listType);
                    deliverSuccess(callback, list);
                } catch (Exception e) {
                    deliverError(callback, e.getMessage());
                }
            }
        });
    }

    // ── Fetch Dynamic Services (Tele2 Style Buttons) ─────────────────────────────
    public void fetchServices(ApiCallback<List<BankService>> callback) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/services")
                .get()
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverError(callback, "Не удалось загрузить сервисы");
                    return;
                }
                try {
                    String respStr = response.body().string();
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    Type listType = new TypeToken<List<BankService>>(){}.getType();
                    List<BankService> list = gson.fromJson(obj.get("services"), listType);
                    deliverSuccess(callback, list);
                } catch (Exception e) {
                    deliverError(callback, e.getMessage());
                }
            }
        });
    }

    // ── Lookup Phone ────────────────────────────────────────────────────────────
    public void lookupPhone(String phone, ApiCallback<User> callback) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/client/lookup_phone?phone=" + phone)
                .get()
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, "Получатель не найден");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverError(callback, "Клиент с таким номером не найден в банке RoPay");
                    return;
                }
                try {
                    String respStr = response.body().string();
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    User u = gson.fromJson(obj.get("user"), User.class);
                    deliverSuccess(callback, u);
                } catch (Exception e) {
                    deliverError(callback, "Ошибка данных получателя");
                }
            }
        });
    }

    // ── Execute Transfer ────────────────────────────────────────────────────────
    public void executeTransfer(int senderId, int recipientId, double amount, ApiCallback<Double> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("sender_id", senderId);
        body.put("recipient_id", recipientId);
        body.put("amount", amount);

        Request req = new Request.Builder()
                .url(BASE_URL + "/api/client/transfer")
                .post(RequestBody.create(gson.toJson(body), JSON_MEDIA))
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, "Ошибка соединения при переводе");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverError(callback, "Перевод отклонен сервером банка");
                    return;
                }
                try {
                    String respStr = response.body().string();
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    if (obj.get("success").getAsBoolean()) {
                        double newBalance = obj.get("new_balance").getAsDouble();
                        deliverSuccess(callback, newBalance);
                    } else {
                        String msg = obj.has("message") ? obj.get("message").getAsString() : "Ошибка перевода";
                        deliverError(callback, msg);
                    }
                } catch (Exception e) {
                    deliverError(callback, "Ошибка обработки ответа банка");
                }
            }
        });
    }

    // ── Support Chat API ────────────────────────────────────────────────────────
    public void initSupportChat(int userId, String userName, String phone, ApiCallback<SupportChat> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("user_name", userName);
        body.put("phone", phone);

        Request req = new Request.Builder()
                .url(BASE_URL + "/api/support/chat/init")
                .post(RequestBody.create(gson.toJson(body), JSON_MEDIA))
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverError(callback, "Не удалось подключиться к поддержке");
                    return;
                }
                try {
                    String respStr = response.body().string();
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    SupportChat sc = gson.fromJson(obj.get("chat"), SupportChat.class);
                    deliverSuccess(callback, sc);
                } catch (Exception e) {
                    deliverError(callback, e.getMessage());
                }
            }
        });
    }

    public void fetchSupportMessages(int chatId, ApiCallback<List<SupportMessage>> callback) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/support/messages?chat_id=" + chatId)
                .get()
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    deliverError(callback, "Ошибка получения сообщений");
                    return;
                }
                try {
                    String respStr = response.body().string();
                    JsonObject obj = gson.fromJson(respStr, JsonObject.class);
                    Type listType = new TypeToken<List<SupportMessage>>(){}.getType();
                    List<SupportMessage> list = gson.fromJson(obj.get("messages"), listType);
                    deliverSuccess(callback, list);
                } catch (Exception e) {
                    deliverError(callback, e.getMessage());
                }
            }
        });
    }

    public void sendSupportMessage(int chatId, String sender, String senderName, String text, ApiCallback<Boolean> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("sender", sender);
        body.put("sender_name", senderName);
        body.put("text", text);

        Request req = new Request.Builder()
                .url(BASE_URL + "/api/support/send")
                .post(RequestBody.create(gson.toJson(body), JSON_MEDIA))
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    deliverSuccess(callback, true);
                } else {
                    deliverError(callback, "Не удалось отправить сообщение");
                }
            }
        });
    }

    public void payTerminalSession(String sessionId, int buyerId, ApiCallback<String> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("session_id", sessionId);
        body.put("buyer_id", buyerId);

        Request req = new Request.Builder()
                .url(BASE_URL + "/api/terminal/pay")
                .post(RequestBody.create(gson.toJson(body), JSON_MEDIA))
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, "Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String str = response.body() != null ? response.body().string() : "";
                    JsonObject obj = gson.fromJson(str, JsonObject.class);
                    boolean success = obj != null && obj.has("success") && obj.get("success").getAsBoolean();
                    String msg = obj != null && obj.has("message") ? obj.get("message").getAsString() : "Оплата завершена";
                    if (success) {
                        deliverSuccess(callback, msg);
                    } else {
                        deliverError(callback, msg);
                    }
                } catch (Exception e) {
                    deliverError(callback, "Ошибка обработки ответа: " + e.getMessage());
                }
            }
        });
    }
}
