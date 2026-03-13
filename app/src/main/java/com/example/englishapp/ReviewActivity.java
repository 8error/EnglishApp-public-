package com.example.englishapp;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.englishapp.entity.Word;
import com.example.englishapp.repository.WordRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ReviewActivity extends AppCompatActivity {

    private static final String TAG = "ReviewActivity";

    private TextView tvWord;
    private TextView tvPhonetic;
    private TextView tvMeaning;
    private TextView tvExample;
    private TextView tvExampleTranslation;
    private TextView tvProgress;
    private CardView cardFront;
    private CardView cardBack;
    private Button btnShowAnswer;
    private Button btnRemember;
    private Button btnForget;
    private Button btnNextWord;  // 新增：下一个单词按钮
    private LinearLayout layoutRememberForget;  // 新增：记住/忘记按钮组
    private ImageView btnSpeak;
    private ProgressBar progressBar;
    private FloatingActionButton fabFlip;
    private Toolbar toolbar;

    private WordRepository wordRepository;
    private CompositeDisposable disposables = new CompositeDisposable();
    private List<Word> reviewWords = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isShowingAnswer = false;
    private TextToSpeech textToSpeech;
    private Animation flipIn;
    private Animation flipOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        // 初始化视图
        initViews();

        // 设置工具栏
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("复习单词");
        }

        // 初始化动画
        initAnimations();

        // 初始化TTS
        initTextToSpeech();

        // 初始化Repository
        wordRepository = new WordRepository(getApplication());

        // 加载需要复习的单词
        loadReviewWords();

        // 设置点击事件
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvWord = findViewById(R.id.tv_word);
        tvPhonetic = findViewById(R.id.tv_phonetic);
        tvMeaning = findViewById(R.id.tv_meaning);
        tvExample = findViewById(R.id.tv_example);
        tvExampleTranslation = findViewById(R.id.tv_example_translation);
        tvProgress = findViewById(R.id.tv_progress);
        cardFront = findViewById(R.id.card_front);
        cardBack = findViewById(R.id.card_back);
        btnShowAnswer = findViewById(R.id.btn_show_answer);
        btnRemember = findViewById(R.id.btn_remember);
        btnForget = findViewById(R.id.btn_forget);
        btnNextWord = findViewById(R.id.btn_next_word);  // 初始化新按钮
        layoutRememberForget = findViewById(R.id.layout_remember_forget);  // 初始化按钮组
        btnSpeak = findViewById(R.id.btn_speak);
        progressBar = findViewById(R.id.progress_bar);
        fabFlip = findViewById(R.id.fab_flip);
    }

    private void initAnimations() {
        flipIn = AnimationUtils.loadAnimation(this, R.anim.flip_in);
        flipOut = AnimationUtils.loadAnimation(this, R.anim.flip_out);
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "语言不支持");
                    btnSpeak.setEnabled(false);
                }
            } else {
                Log.e(TAG, "TTS初始化失败");
                btnSpeak.setEnabled(false);
            }
        });
    }

    private void loadReviewWords() {
        progressBar.setVisibility(View.VISIBLE);

        // 获取每日复习数量设置
        int dailyLimit = SettingsActivity.getDailyReviewCount(this);
        boolean isStrictMode = SettingsActivity.isStrictMode(this);

        disposables.add(
                wordRepository.getWordsToReview()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                words -> {
                                    // 根据每日复习数量限制筛选单词
                                    if (words.size() > dailyLimit) {
                                        if (isStrictMode) {
                                            // 严格模式：只复习前dailyLimit个
                                            reviewWords = words.subList(0, dailyLimit);
                                            Toast.makeText(this,
                                                    "今日有" + words.size() + "个单词待复习，已按设置显示前" + dailyLimit + "个",
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            // 灵活模式：全部复习，但提示用户
                                            reviewWords = words;
                                            Toast.makeText(this,
                                                    "今日有" + words.size() + "个单词需要复习",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    } else {
                                        reviewWords = words;
                                    }

                                    progressBar.setVisibility(View.GONE);

                                    if (reviewWords.isEmpty()) {
                                        showNoWordsMessage();
                                    } else {
                                        showCurrentWord();
                                    }
                                },
                                throwable -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(this, "加载失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                        )
        );
    }

    private void showNoWordsMessage() {
        Toast.makeText(this, "没有需要复习的单词", Toast.LENGTH_LONG).show();
        new Handler().postDelayed(this::finish, 1500);
    }

    private void showCurrentWord() {
        if (reviewWords.isEmpty() || currentIndex >= reviewWords.size()) {
            finishReview();
            return;
        }

        Word currentWord = reviewWords.get(currentIndex);

        // 更新正面卡片
        tvWord.setText(currentWord.getEnglishWord());
        tvPhonetic.setText("/" + currentWord.getPhonetic() + "/");

        // 更新背面卡片
        tvMeaning.setText(currentWord.getChineseMeaning());
        tvExample.setText(currentWord.getExampleSentence());
        tvExampleTranslation.setText(currentWord.getExampleTranslation());

        // 更新进度
        updateProgress();

        // 确保显示正面
        if (isShowingAnswer) {
            flipCard();
        }
        cardFront.setVisibility(View.VISIBLE);
        cardBack.setVisibility(View.GONE);
        isShowingAnswer = false;

        // 隐藏记住/忘记按钮组（因为还没显示答案）
        layoutRememberForget.setVisibility(View.GONE);

        // 启用显示答案按钮和下一个按钮
        btnShowAnswer.setEnabled(true);
        btnNextWord.setEnabled(true);

        // 自动朗读单词
        speakWord(currentWord.getEnglishWord());
    }

    private void updateProgress() {
        String progress = String.format("%d / %d", currentIndex + 1, reviewWords.size());
        tvProgress.setText(progress);
    }

    private void setupClickListeners() {
        btnShowAnswer.setOnClickListener(v -> showAnswer());

        btnRemember.setOnClickListener(v -> handleReviewResult(true));

        btnForget.setOnClickListener(v -> handleReviewResult(false));

        // 新增：下一个单词按钮点击事件
        btnNextWord.setOnClickListener(v -> nextWord());

        btnSpeak.setOnClickListener(v -> {
            if (!reviewWords.isEmpty() && currentIndex < reviewWords.size()) {
                speakWord(reviewWords.get(currentIndex).getEnglishWord());
            }
        });

        fabFlip.setOnClickListener(v -> flipCard());
    }

    private void showAnswer() {
        if (!isShowingAnswer) {
            flipCard();
            // 显示答案后，显示记住/忘记按钮组
            layoutRememberForget.setVisibility(View.VISIBLE);
            // 可以隐藏显示答案按钮（可选）
            // btnShowAnswer.setVisibility(View.GONE);
        }
    }

    private void flipCard() {
        if (cardFront.getVisibility() == View.VISIBLE) {
            cardFront.startAnimation(flipOut);
            cardBack.startAnimation(flipIn);
            cardFront.setVisibility(View.GONE);
            cardBack.setVisibility(View.VISIBLE);
            isShowingAnswer = true;
        } else {
            cardBack.startAnimation(flipOut);
            cardFront.startAnimation(flipIn);
            cardBack.setVisibility(View.GONE);
            cardFront.setVisibility(View.VISIBLE);
            isShowingAnswer = false;
        }
    }

    private void handleReviewResult(boolean remembered) {
        if (reviewWords.isEmpty() || currentIndex >= reviewWords.size()) {
            return;
        }

        Word currentWord = reviewWords.get(currentIndex);

        disposables.add(
                wordRepository.updateWordMastery(currentWord, remembered)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Toast.makeText(this,
                                            remembered ? "已标记为记住" : "已标记为忘记",
                                            Toast.LENGTH_SHORT).show();

                                    // 检查是否开启自动下一个
                                    if (SettingsActivity.isAutoNext(this)) {
                                        // 自动进入下一个单词
                                        moveToNextWord();
                                    } else {
                                        // 不自动进入，但可以继续操作
                                        // 这里可以保持当前单词，让用户手动点击下一个
                                    }
                                },
                                throwable -> {
                                    Toast.makeText(this, "更新失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                        )
        );
    }

    // 新增：下一个单词按钮的处理方法
    private void nextWord() {
        // 如果还没显示答案，直接跳过
        if (!isShowingAnswer) {
            moveToNextWord();
        } else {
            // 如果已经显示答案，需要先处理当前单词的复习状态
            // 这里可以选择：1. 自动标记为忘记 2. 提示用户先选择记住/忘记
            // 我们选择提示用户
            Toast.makeText(this, "请先选择「记住」或「忘记」", Toast.LENGTH_SHORT).show();
        }
    }

    // 新增：移动到下一个单词的逻辑
    private void moveToNextWord() {
        if (currentIndex < reviewWords.size() - 1) {
            currentIndex++;
            showCurrentWord();
        } else {
            // 已经是最后一个单词
            finishReview();
        }
    }

    private void speakWord(String word) {
        if (textToSpeech != null) {
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void finishReview() {
        Toast.makeText(this, "恭喜！今日复习完成！", Toast.LENGTH_LONG).show();
        new Handler().postDelayed(this::finish, 1500);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}