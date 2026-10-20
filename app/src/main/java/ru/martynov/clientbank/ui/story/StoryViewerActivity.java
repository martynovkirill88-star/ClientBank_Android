package ru.martynov.clientbank.ui.story;

import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import java.util.ArrayList;
import java.util.List;
import ru.martynov.clientbank.R;
import ru.martynov.clientbank.data.api.ApiClient;
import ru.martynov.clientbank.data.local.SessionManager;
import ru.martynov.clientbank.data.models.BankStatus;

public class StoryViewerActivity extends AppCompatActivity {
    private ImageView ivPhoto;
    private VideoView vvVideo;
    private ProgressBar pbLoading;
    private LinearLayout layoutProgressBars;
    private TextView tvTitle;
    private TextView tvCaption;

    private List<BankStatus> storiesList = new ArrayList<>();
    private final List<ProgressBar> segmentBars = new ArrayList<>();
    private int currentIndex = 0;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private long storyDurationMs = 8000;
    private long elapsedMs = 0;
    private boolean isPaused = false;
    private static final int TICK_INTERVAL = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);

        ivPhoto = findViewById(R.id.iv_story_photo);
        vvVideo = findViewById(R.id.vv_story_video);
        pbLoading = findViewById(R.id.pb_story_loading);
        layoutProgressBars = findViewById(R.id.layout_progress_bars);
        tvTitle = findViewById(R.id.tv_viewer_title);
        tvCaption = findViewById(R.id.tv_viewer_caption);

        findViewById(R.id.btn_story_close).setOnClickListener(v -> finish());

        List<BankStatus> list = (List<BankStatus>) getIntent().getSerializableExtra("statuses_list");
        if (list != null) storiesList = list;
        currentIndex = getIntent().getIntExtra("selected_index", 0);

        if (storiesList.isEmpty()) {
            finish();
            return;
        }

        setupProgressBars();
        setupTouchControls();

        showStory(currentIndex);
    }

    private void setupProgressBars() {
        layoutProgressBars.removeAllViews();
        segmentBars.clear();

        for (int i = 0; i < storiesList.size(); i++) {
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            params.setMargins(4, 0, 4, 0);
            bar.setLayoutParams(params);
            bar.setMax(1000);
            bar.setProgress(0);
            bar.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.bg_button_mint));
            layoutProgressBars.addView(bar);
            segmentBars.add(bar);
        }
    }

    private void setupTouchControls() {
        View touchLeft = findViewById(R.id.touch_left_prev);
        View touchRight = findViewById(R.id.touch_right_next);

        touchLeft.setOnClickListener(v -> previousStory());
        touchRight.setOnClickListener(v -> nextStory());

        View.OnTouchListener holdListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pauseStory();
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    resumeStory();
                    return false;
            }
            return false;
        };

        touchLeft.setOnTouchListener(holdListener);
        touchRight.setOnTouchListener(holdListener);
    }

    private void showStory(int index) {
        if (index < 0 || index >= storiesList.size()) {
            finish();
            return;
        }

        stopProgress();
        currentIndex = index;

        for (int i = 0; i < segmentBars.size(); i++) {
            if (i < currentIndex) {
                segmentBars.get(i).setProgress(1000);
            } else {
                segmentBars.get(i).setProgress(0);
            }
        }

        BankStatus story = storiesList.get(index);
        SessionManager.getInstance().markStoryViewed(story.id);

        tvTitle.setText(story.title != null && !story.title.isEmpty() ? story.title : "RoPay Банк");
        if (story.text != null && !story.text.trim().isEmpty()) {
            tvCaption.setText(story.text.trim());
            tvCaption.setVisibility(View.VISIBLE);
        } else {
            tvCaption.setVisibility(View.GONE);
        }

        String url = story.mediaUrl != null ? story.mediaUrl : "";
        if (!url.isEmpty() && !url.startsWith("http")) {
            url = ApiClient.BASE_URL + url;
        }

        boolean isVideo = "video".equalsIgnoreCase(story.mediaType);
        storyDurationMs = story.getEffectiveDuration() * 1000L;
        if (storyDurationMs < 3000) storyDurationMs = 8000;

        elapsedMs = 0;
        pbLoading.setVisibility(View.VISIBLE);

        if (isVideo) {
            ivPhoto.setVisibility(View.GONE);
            vvVideo.setVisibility(View.VISIBLE);

            try {
                vvVideo.setVideoURI(Uri.parse(url));
                vvVideo.setOnPreparedListener(mp -> {
                    pbLoading.setVisibility(View.GONE);
                    mp.setLooping(false);
                    long vidDur = vvVideo.getDuration();
                    if (vidDur > 1000) {
                        storyDurationMs = vidDur;
                    }
                    vvVideo.start();
                    startProgress();
                });
                vvVideo.setOnCompletionListener(mp -> nextStory());
                vvVideo.setOnErrorListener((mp, what, extra) -> {
                    pbLoading.setVisibility(View.GONE);
                    nextStory();
                    return true;
                });
            } catch (Exception e) {
                pbLoading.setVisibility(View.GONE);
                startProgress();
            }
        } else {
            vvVideo.setVisibility(View.GONE);
            ivPhoto.setVisibility(View.VISIBLE);

            if (!url.isEmpty()) {
                Glide.with(this)
                        .load(url)
                        .fitCenter()
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                pbLoading.setVisibility(View.GONE);
                                startProgress();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                pbLoading.setVisibility(View.GONE);
                                startProgress();
                                return false;
                            }
                        })
                        .into(ivPhoto);
            } else {
                pbLoading.setVisibility(View.GONE);
                startProgress();
            }
        }
    }

    private void startProgress() {
        stopProgress();
        isPaused = false;

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPaused) {
                    elapsedMs += TICK_INTERVAL;
                    float ratio = (float) elapsedMs / (float) storyDurationMs;
                    if (currentIndex < segmentBars.size()) {
                        segmentBars.get(currentIndex).setProgress((int) (ratio * 1000));
                    }

                    if (elapsedMs >= storyDurationMs) {
                        nextStory();
                        return;
                    }
                }
                progressHandler.postDelayed(this, TICK_INTERVAL);
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgress() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    private void pauseStory() {
        isPaused = true;
        if (vvVideo.isPlaying()) {
            vvVideo.pause();
        }
    }

    private void resumeStory() {
        isPaused = false;
        if (vvVideo.getVisibility() == View.VISIBLE && !vvVideo.isPlaying()) {
            vvVideo.start();
        }
    }

    private void nextStory() {
        if (currentIndex + 1 < storiesList.size()) {
            showStory(currentIndex + 1);
        } else {
            finish();
        }
    }

    private void previousStory() {
        if (currentIndex - 1 >= 0) {
            showStory(currentIndex - 1);
        } else {
            showStory(0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseStory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeStory();
    }

    @Override
    protected void onDestroy() {
        stopProgress();
        if (vvVideo != null) {
            vvVideo.stopPlayback();
        }
        super.onDestroy();
    }
}
