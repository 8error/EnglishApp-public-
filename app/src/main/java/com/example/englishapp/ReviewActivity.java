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
        disposables.add(
                wordRepository.getWordsToReview()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                words -> {
                                    reviewWords = words;
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
                                    // 移动到下一个单词
                                    currentIndex++;
                                    if (currentIndex < reviewWords.size()) {
                                        showCurrentWord();
                                    } else {
                                        finishReview();
                                    }
                                },
                                throwable -> {
                                    Toast.makeText(this, "更新失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                        )
        );
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