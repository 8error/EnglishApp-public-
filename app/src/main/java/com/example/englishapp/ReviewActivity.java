package com.example.englishapp;

import android.content.SharedPreferences;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.englishapp.entity.Word;
import com.example.englishapp.repository.WordRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ReviewActivity extends AppCompatActivity {

    private static final String TAG = "ReviewActivity";

    private TextView tvWord;
    private TextView tvWordBack;          // 背面卡片单词
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
    private LinearLayout layoutRememberForget;
    private ImageView btnSpeak;            // 正面语音按钮
    private ImageView btnSpeakBack;        // 背面语音按钮
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private WordRepository wordRepository;
    private CompositeDisposable disposables = new CompositeDisposable();

    // 使用队列管理复习单词
    private Queue<Word> reviewQueue = new LinkedList<>();
    private List<Word> forgottenWords = new ArrayList<>();
    private Word currentWord;
    private int totalPlanCount = 0;
    private int sessionReviewedCount = 0;
    private int rememberedCount = 0;

    private boolean isShowingAnswer = false;
    private TextToSpeech textToSpeech;
    private Animation flipIn;
    private Animation flipOut;

    private boolean isNewGroup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        initViews();
        setupToolbar();
        initAnimations();
        initTextToSpeech();

        wordRepository = new WordRepository(getApplication());

        loadReviewWords();
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvWord = findViewById(R.id.tv_word);
        tvWordBack = findViewById(R.id.tv_word_back);
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
        layoutRememberForget = findViewById(R.id.layout_remember_forget);
        btnSpeak = findViewById(R.id.btn_speak);
        btnSpeakBack = findViewById(R.id.btn_speak_back);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("复习单词");
        }
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
                    if (btnSpeak != null) btnSpeak.setEnabled(false);
                    if (btnSpeakBack != null) btnSpeakBack.setEnabled(false);
                }
            } else {
                Log.e(TAG, "TTS初始化失败");
                if (btnSpeak != null) btnSpeak.setEnabled(false);
                if (btnSpeakBack != null) btnSpeakBack.setEnabled(false);
            }
        });
    }

    private String getTodayDateString() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA);
        return sdf.format(new java.util.Date());
    }

    private void saveReviewedCount() {
        SharedPreferences prefs = getSharedPreferences("ReviewProgress", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        int reviewedToday = prefs.getInt("reviewed_today_" + getTodayDateString(), 0);
        reviewedToday++;
        editor.putInt("reviewed_today_" + getTodayDateString(), reviewedToday);
        editor.apply();
        Log.d(TAG, "保存今日已复习数量: " + reviewedToday);
    }

    private int getTodayReviewedCount() {
        SharedPreferences prefs = getSharedPreferences("ReviewProgress", MODE_PRIVATE);
        return prefs.getInt("reviewed_today_" + getTodayDateString(), 0);
    }

    private void loadReviewWords() {
        progressBar.setVisibility(View.VISIBLE);

        int dailyLimit = SettingsActivity.getDailyReviewCount(this);
        boolean isStrictMode = SettingsActivity.isStrictMode(this);
        boolean isRandomOrder = SettingsActivity.isRandomOrder(this);
        String today = getTodayDateString();

        disposables.add(
                wordRepository.getWordsToReview()
                        .firstOrError()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                words -> {
                                    List<Word> learnedWords = new ArrayList<>();
                                    for (Word word : words) {
                                        if (word.getMasteryLevel() > 0 || word.getReviewCount() > 0) {
                                            learnedWords.add(word);
                                        }
                                    }

                                    Log.d(TAG, "需要复习的单词: " + words.size() +
                                            "，已学习的: " + learnedWords.size());

                                    int reviewedToday = getTodayReviewedCount();

                                    SharedPreferences prefs = getSharedPreferences("ReviewList", MODE_PRIVATE);
                                    String savedDate = prefs.getString("review_date", "");
                                    String savedWordIds = prefs.getString("word_ids", "");
                                    boolean isReset = prefs.getBoolean("is_reset", false);

                                    List<Word> wordsToReview;

                                    if (savedDate.equals(today) && !savedWordIds.isEmpty() && !isReset) {
                                        wordsToReview = loadReviewListFromSavedIds(savedWordIds, learnedWords);
                                        if (wordsToReview.isEmpty()) {
                                            wordsToReview = generateNewReviewList(learnedWords, dailyLimit, isStrictMode, isRandomOrder);
                                            saveTodayReviewList(wordsToReview, today);
                                            isNewGroup = true;
                                        } else {
                                            Log.d(TAG, "从保存的列表加载了 " + wordsToReview.size() + " 个单词");
                                        }
                                    } else {
                                        wordsToReview = generateNewReviewList(learnedWords, dailyLimit, isStrictMode, isRandomOrder);
                                        saveTodayReviewList(wordsToReview, today);
                                        SharedPreferences progressPrefs = getSharedPreferences("ReviewProgress", MODE_PRIVATE);
                                        progressPrefs.edit().remove("reviewed_today_" + today).apply();
                                        reviewedToday = 0;
                                        if (isReset) {
                                            prefs.edit().remove("is_reset").apply();
                                        }
                                        isNewGroup = true;
                                        Log.d(TAG, "生成了新的复习列表，共 " + wordsToReview.size() + " 个单词");
                                    }

                                    totalPlanCount = wordsToReview.size();

                                    List<Word> remainingWords;
                                    if (reviewedToday > 0 && reviewedToday < wordsToReview.size()) {
                                        remainingWords = wordsToReview.subList(reviewedToday, wordsToReview.size());
                                        Log.d(TAG, "跳过已复习的 " + reviewedToday + " 个单词，剩余 " + remainingWords.size() + " 个");
                                    } else if (reviewedToday >= wordsToReview.size()) {
                                        remainingWords = new ArrayList<>();
                                        Log.d(TAG, "今日所有单词都已复习完成");
                                    } else {
                                        remainingWords = wordsToReview;
                                    }

                                    reviewQueue.clear();
                                    reviewQueue.addAll(remainingWords);
                                    sessionReviewedCount = 0;
                                    rememberedCount = 0;

                                    progressBar.setVisibility(View.GONE);

                                    if (reviewQueue.isEmpty()) {
                                        showNoWordsMessage();
                                    } else {
                                        showNextWord();
                                    }
                                },
                                throwable -> {
                                    progressBar.setVisibility(View.GONE);
                                    Log.e(TAG, "加载复习单词失败", throwable);
                                    Toast.makeText(ReviewActivity.this, "加载失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                        )
        );
    }

    private void showNextWord() {
        if (reviewQueue.isEmpty()) {
            if (!forgottenWords.isEmpty()) {
                reviewQueue.addAll(forgottenWords);
                forgottenWords.clear();
                Log.d(TAG, "将忘记的单词重新加入队列，数量: " + reviewQueue.size());
            } else {
                finishReview();
                return;
            }
        }

        currentWord = reviewQueue.poll();
        sessionReviewedCount++;

        // 更新UI
        tvWord.setText(currentWord.getEnglishWord());
        if (tvWordBack != null) {
            tvWordBack.setText(currentWord.getEnglishWord());
        }
        tvPhonetic.setText("/" + (currentWord.getPhonetic() != null ? currentWord.getPhonetic() : "") + "/");
        tvMeaning.setText(currentWord.getChineseMeaning());
        tvExample.setText(currentWord.getExampleSentence() != null ? currentWord.getExampleSentence() : "");
        tvExampleTranslation.setText(currentWord.getExampleTranslation() != null ? currentWord.getExampleTranslation() : "");

        Log.d(TAG, "当前单词: " + currentWord.getEnglishWord() +
                ", 掌握程度: " + currentWord.getMasteryLevel() +
                ", 队列剩余: " + reviewQueue.size());

        cardFront.setVisibility(View.VISIBLE);
        cardBack.setVisibility(View.GONE);
        isShowingAnswer = false;

        layoutRememberForget.setVisibility(View.GONE);
        btnShowAnswer.setText("显示答案");
        btnShowAnswer.setEnabled(true);

        updateProgress();
        speakWord(currentWord.getEnglishWord());
    }

    private void updateProgress() {
        int reviewedToday = getTodayReviewedCount();
        int completed = reviewedToday + rememberedCount;
        int total = totalPlanCount;
        completed = Math.min(completed, total);
        completed = Math.max(completed, 0);
        tvProgress.setText(String.format("%d / %d", completed, total));
    }

    private void setupClickListeners() {
        btnShowAnswer.setOnClickListener(v -> flipCard());

        btnRemember.setOnClickListener(v -> handleReviewResult(true));
        btnForget.setOnClickListener(v -> handleReviewResult(false));

        // 正面语音按钮
        if (btnSpeak != null) {
            btnSpeak.setOnClickListener(v -> {
                if (currentWord != null) {
                    speakWord(currentWord.getEnglishWord());
                }
            });
        }

        // 背面语音按钮
        if (btnSpeakBack != null) {
            btnSpeakBack.setOnClickListener(v -> {
                if (currentWord != null) {
                    speakWord(currentWord.getEnglishWord());
                }
            });
        }

        cardFront.setOnClickListener(v -> flipCard());
        cardBack.setOnClickListener(v -> flipCard());
    }

    private void flipCard() {
        if (cardFront.getVisibility() == View.VISIBLE) {
            cardFront.startAnimation(flipOut);
            cardBack.startAnimation(flipIn);
            cardFront.setVisibility(View.GONE);
            cardBack.setVisibility(View.VISIBLE);
            isShowingAnswer = true;

            layoutRememberForget.setVisibility(View.VISIBLE);
            btnShowAnswer.setText("返回正面");
        } else {
            cardBack.startAnimation(flipOut);
            cardFront.startAnimation(flipIn);
            cardBack.setVisibility(View.GONE);
            cardFront.setVisibility(View.VISIBLE);
            isShowingAnswer = false;

            layoutRememberForget.setVisibility(View.GONE);
            btnShowAnswer.setText("显示答案");
        }
    }

    private void handleReviewResult(boolean remembered) {
        if (currentWord == null) return;

        if (remembered) {
            rememberedCount++;
            saveReviewedCount();
            updateWordMastery(currentWord, true);
            Log.d(TAG, "记住单词，当前记住计数: " + rememberedCount);
        } else {
            forgottenWords.add(currentWord);
            Toast.makeText(this, "已加入待重练列表", Toast.LENGTH_SHORT).show();
            updateWordMastery(currentWord, false);
            Log.d(TAG, "忘记单词，当前忘记列表大小: " + forgottenWords.size());
        }
        showNextWord();
    }

    private void updateWordMastery(Word word, boolean remembered) {
        disposables.add(
                wordRepository.updateWordMastery(word, remembered)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Log.d(TAG, "单词 " + word.getEnglishWord() + " 更新成功"),
                                throwable -> Log.e(TAG, "更新失败", throwable)
                        )
        );
    }

    private void speakWord(String word) {
        if (textToSpeech != null && word != null && !word.isEmpty()) {
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private List<Word> generateNewReviewList(List<Word> allWords, int dailyLimit, boolean isStrictMode, boolean isRandomOrder) {
        List<Word> newList;
        List<Word> allReviewWords = new ArrayList<>(allWords);
        if (isRandomOrder) {
            Collections.shuffle(allReviewWords);
        }
        if (allReviewWords.size() > dailyLimit) {
            if (isStrictMode) {
                newList = allReviewWords.subList(0, dailyLimit);
            } else {
                newList = allReviewWords;
            }
        } else {
            newList = allReviewWords;
        }
        return newList;
    }

    private void saveTodayReviewList(List<Word> reviewWords, String today) {
        StringBuilder idBuilder = new StringBuilder();
        for (int i = 0; i < reviewWords.size(); i++) {
            if (i > 0) idBuilder.append(",");
            idBuilder.append(reviewWords.get(i).getId());
        }
        SharedPreferences prefs = getSharedPreferences("ReviewList", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("review_date", today);
        editor.putString("word_ids", idBuilder.toString());
        editor.apply();
        Log.d(TAG, "已保存今日复习列表，共 " + reviewWords.size() + " 个单词");
    }

    private List<Word> loadReviewListFromSavedIds(String savedWordIds, List<Word> allWords) {
        List<Word> loadedList = new ArrayList<>();
        String[] idArray = savedWordIds.split(",");
        java.util.Map<Integer, Word> wordMap = new java.util.HashMap<>();
        for (Word word : allWords) {
            wordMap.put(word.getId(), word);
        }
        for (String idStr : idArray) {
            try {
                int id = Integer.parseInt(idStr);
                Word word = wordMap.get(id);
                if (word != null) {
                    loadedList.add(word);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "解析ID失败: " + idStr);
            }
        }
        return loadedList;
    }

    private void showNoWordsMessage() {
        Toast.makeText(this, "没有需要复习的单词", Toast.LENGTH_LONG).show();
        new Handler().postDelayed(this::finish, 1500);
    }

    private void finishReview() {
        disposables.add(
                wordRepository.getWordsToReview()
                        .firstOrError()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                words -> {
                                    List<Word> learnedWords = new ArrayList<>();
                                    for (Word word : words) {
                                        if (word.getMasteryLevel() > 0 || word.getReviewCount() > 0) {
                                            learnedWords.add(word);
                                        }
                                    }
                                    int totalNeedReview = learnedWords.size();
                                    int reviewedToday = getTodayReviewedCount();
                                    int remaining = Math.max(0, totalPlanCount - reviewedToday);

                                    if (remaining > 0 && isNewGroup) {
                                        new AlertDialog.Builder(this)
                                                .setTitle("复习完成")
                                                .setMessage("您已完成本组复习！\n\n还有 " + remaining + " 个单词需要复习，是否继续？")
                                                .setPositiveButton("继续复习", (dialog, which) -> {
                                                    resetReviewList();
                                                    loadReviewWords();
                                                })
                                                .setNegativeButton("结束复习", (dialog, which) -> finish())
                                                .setCancelable(false)
                                                .show();
                                    } else if (remaining > 0) {
                                        Toast.makeText(this, "本组复习完成！还有 " + remaining + " 个单词待复习", Toast.LENGTH_LONG).show();
                                        finish();
                                    } else {
                                        Toast.makeText(this, "恭喜！今日所有复习任务已完成！", Toast.LENGTH_LONG).show();
                                        new Handler().postDelayed(this::finish, 1500);
                                    }
                                },
                                throwable -> {
                                    Toast.makeText(this, "恭喜！今日复习完成！", Toast.LENGTH_LONG).show();
                                    new Handler().postDelayed(this::finish, 1500);
                                }
                        )
        );
    }

    private void resetReviewList() {
        SharedPreferences prefs = getSharedPreferences("ReviewList", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("review_date");
        editor.remove("word_ids");
        editor.putBoolean("is_reset", true);
        editor.apply();
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