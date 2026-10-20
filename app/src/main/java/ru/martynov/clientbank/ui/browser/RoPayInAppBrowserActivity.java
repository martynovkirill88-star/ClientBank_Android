package ru.martynov.clientbank.ui.browser;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Map;
import ru.martynov.clientbank.R;

public class RoPayInAppBrowserActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvTitle;

    private static final String APP_USER_AGENT = "RoPayClientApp/1.0.22 (Android; Mobile) RoPaySecureInAppBrowser";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_browser);

        webView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.progress_bar);
        tvTitle = findViewById(R.id.tv_browser_title);

        String targetUrl = getIntent().getStringExtra("target_url");
        String title = getIntent().getStringExtra("title");
        int userId = getIntent().getIntExtra("user_id", 0);

        if (title != null && !title.isEmpty()) {
            tvTitle.setText(title);
        }

        findViewById(R.id.btn_browser_close).setOnClickListener(v -> finish());
        findViewById(R.id.btn_browser_refresh).setOnClickListener(v -> webView.reload());

        // Configure WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUserAgentString(APP_USER_AGENT);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // Lock Zoom & Pinch (Native App Experience)
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                String js = "javascript:(function() {" +
                        "var meta = document.querySelector('meta[name=\"viewport\"]');" +
                        "if (!meta) { meta = document.createElement('meta'); meta.name = 'viewport'; if (document.head) document.head.appendChild(meta); }" +
                        "if (meta) { meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no, shrink-to-fit=no'; }" +
                        "document.body.style.touchAction = 'pan-x pan-y';" +
                        "})()";
                view.evaluateJavascript(js, null);
            }
        });

        if (targetUrl == null || targetUrl.isEmpty()) {
            targetUrl = "https://app.ropay.site";
        }
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            targetUrl = "https://" + targetUrl;
        }

        // Add secure in-app query parameters
        Uri.Builder uriBuilder = Uri.parse(targetUrl).buildUpon();
        uriBuilder.appendQueryParameter("ropay_inapp", "1");
        if (userId > 0) {
            uriBuilder.appendQueryParameter("uid", String.valueOf(userId));
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("X-RoPay-App", "true");
        headers.put("X-RoPay-Version", "1.0.22");

        webView.loadUrl(uriBuilder.build().toString(), headers);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
