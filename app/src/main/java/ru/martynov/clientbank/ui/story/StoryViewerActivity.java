package ru.martynov.clientbank.ui.story;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.BankStatus;

public class StoryViewerActivity extends AppCompatActivity {
    private ImageView ivMedia;
    private ProgressBar pbTimer;
    private TextView tvTitle;
    private TextView tvText;

    private List<BankStatus> storiesList = new ArrayList<>();
    private int currentIndex = 0;
    private CountDownTimer storyTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);

        ivMedia = findViewById(R.id.iv_story_full);
        pbTimer = findViewById(R.id.pb_story_timer);
        tvTitle = findViewById(R.id.tv_story_viewer_title);
        tvText = findViewById(R.id.tv_story_text);

        findViewById(R.id.btn_story_close).setOnClickListener(v -> finish());

        List<BankStatus> list = (List<BankStatus>) getIntent().getSerializableExtra("statuses_list");
        if (list != null) storiesList = list;
        currentIndex = getIntent().getIntExtra("selected_index", 0);

        if (storiesList.isEmpty()) {
            finish();
            return;
        }

        findViewById(R.id.iv_story_full).setOnClickListener(v -> nextStory());

        showStory(currentIndex);
    }

    private void showStory(int index) {
        if (index >= storiesList.size()) {
            finish();
            return;
        }

        if (storyTimer != null) storyTimer.cancel();

        BankStatus story = storiesList.get(index);
        SessionManager.getInstance().markStoryViewed(story.id);

        tvTitle.setText(story.title != null ? story.title : "RoPay Банк");
        tvText.setText(story.text != null ? story.text : "");

        String mediaUrl = story.mediaUrl != null ? story.mediaUrl : "";
        if (!mediaUrl.startsWith("http")) {
            mediaUrl = ApiClient.BASE_URL + mediaUrl;
        }

        Glide.with(this)
                .load(mediaUrl)
                .centerCrop()
                .into(ivMedia);

        int durationSec = story.getEffectiveDuration();
        long totalMs = durationSec * 1000L;
        pbTimer.setMax((int) totalMs);
        pbTimer.setProgress(0);

        storyTimer = new CountDownTimer(totalMs, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                pbTimer.setProgress((int) (totalMs - millisUntilFinished));
            }

            @Override
            public void onFinish() {
                pbTimer.setProgress((int) totalMs);
                nextStory();
            }
        }.start();
    }

    private void nextStory() {
        currentIndex++;
        if (currentIndex < storiesList.size()) {
            showStory(currentIndex);
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (storyTimer != null) storyTimer.cancel();
    }
}
