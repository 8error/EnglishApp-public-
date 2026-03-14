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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private ImageView btnSpeak;
    private ProgressBar progressBar;
    private FloatingActionButton fabFlip;
    private Toolbar toolbar;

    private WordRepository wordRepository;
    private CompositeDisposable disposables = new CompositeDisposable();

    // 使用队列管理复习单词
    private Queue<Word> reviewQueue = new LinkedList<>();
    private List<Word> forgottenWords = new ArrayList<>(); // 忘记的单词暂存区
    private Word currentWord;
    private int totalWordCount = 0;
    private int reviewedCount = 0;

    private boolean isShowingAnswer = false;
    private TextToSpeech textToSpeech;
    private Animation flipIn;
    private Animation flipOut;

    // 标记是否是从"再学一组"进来的
    private boolean isNewGroup = false;

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
        layoutRememberForget = findViewById(R.id.layout_remember_forget);
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
        boolean isRandomOrder = SettingsActivity.isRandomOrder(this);

        // 获取今天的日期
        String today = getTodayDateString();

        disposables.add(
                wordRepository.getWordsToReview()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                words -> {
                                    // 检查是否已有今日的复习列表
                                    SharedPreferences prefs = getSharedPreferences("ReviewList", MODE_PRIVATE);
                                    String savedDate = prefs.getString("review_date", "");
                                    String savedWordIds = prefs.getString("word_ids", "");

                                    // 检查是否是被重置的状态
                                    boolean isReset = prefs.getBoolean("is_reset", false);

                                    List<Word> wordsToReview;

                                    if (savedDate.equals(today) && !savedWordIds.isEmpty() && !isReset) {
                                        // 今天已经生成过复习列表，从保存的ID加载
                                        wordsToReview = loadReviewListFromSavedIds(savedWordIds, words);

                                        // 如果加载的列表为空，重新生成
                                        if (wordsToReview.isEmpty()) {
                                            wordsToReview = generateNewReviewList(words, dailyLimit, isStrictMode, isRandomOrder);
                                            saveTodayReviewList(wordsToReview, today);
                                            isNewGroup = true;
                                        }
                                    } else {
                                        // 今天第一次点击复习，或者被重置了，生成新的复习列表
                                        wordsToReview = generateNewReviewList(words, dailyLimit, isStrictMode, isRandomOrder);

                                        // 保存今天的复习列表
                                        saveTodayReviewList(wordsToReview, today);

                                        // 清除重置标记
                                        if (isReset) {
                                            prefs.edit().remove("is_reset").apply();
                                        }

                                        isNewGroup = true;
                                    }

                                    // 将单词加入队列
                                    reviewQueue.clear();
                                    reviewQueue.addAll(wordsToReview);
                                    totalWordCount = reviewQueue.size();
                                    reviewedCount = 0;

                                    progressBar.setVisibility(View.GONE);

                                    if (reviewQueue.isEmpty()) {
                                        showNoWordsMessage();
                                    } else {
                                        showNextWord();
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

    /**
     * 显示下一个单词
     */
    private void showNextWord() {
        if (reviewQueue.isEmpty()) {
            // 如果队列为空但还有忘记的单词，把忘记的单词重新加入队列
            if (!forgottenWords.isEmpty()) {
                reviewQueue.addAll(forgottenWords);
                forgottenWords.clear();
                Log.d(TAG, "将忘记的单词重新加入队列，数量: " + reviewQueue.size());
            } else {
                // 真的没有单词了
                finishReview();
                return;
            }
        }

        currentWord = reviewQueue.poll();
        reviewedCount++;

        // 更新UI
        tvWord.setText(currentWord.getEnglishWord());
        tvPhonetic.setText("/" + currentWord.getPhonetic() + "/");
        tvMeaning.setText(currentWord.getChineseMeaning());
        tvExample.setText(currentWord.getExampleSentence());
        tvExampleTranslation.setText(currentWord.getExampleTranslation());

        // 确保显示正面
        cardFront.setVisibility(View.VISIBLE);
        cardBack.setVisibility(View.GONE);
        isShowingAnswer = false;

        // 隐藏记住/忘记按钮
        layoutRememberForget.setVisibility(View.GONE);
        btnShowAnswer.setText("翻转卡片");
        btnShowAnswer.setEnabled(true);

        // 更新进度
        updateProgress();

        // 自动朗读单词
        speakWord(currentWord.getEnglishWord());
    }

    /**
     * 更新进度显示
     */
    private void updateProgress() {
        int completed = reviewedCount - 1; // 已完成的单词数（当前显示的还没完成）
        int total = totalWordCount + forgottenWords.size(); // 总单词数包括忘记后重来的
        tvProgress.setText(String.format("%d / %d", completed, total));
    }

    private void setupClickListeners() {
        btnShowAnswer.setOnClickListener(v -> flipCard());

        btnRemember.setOnClickListener(v -> handleReviewResult(true));

        btnForget.setOnClickListener(v -> handleReviewResult(false));

        btnSpeak.setOnClickListener(v -> {
            if (currentWord != null) {
                speakWord(currentWord.getEnglishWord());
            }
        });

        fabFlip.setOnClickListener(v -> flipCard());
        cardFront.setOnClickListener(v -> flipCard());
        cardBack.setOnClickListener(v -> flipCard());
    }

    private void flipCard() {
        if (cardFront.getVisibility() == View.VISIBLE) {
            // 正面 -> 背面
            cardFront.startAnimation(flipOut);
            cardBack.startAnimation(flipIn);
            cardFront.setVisibility(View.GONE);
            cardBack.setVisibility(View.VISIBLE);
            isShowingAnswer = true;

            // 显示背面时：显示记住/忘记按钮
            layoutRememberForget.setVisibility(View.VISIBLE);
            btnShowAnswer.setText("返回正面");
        } else {
            // 背面 -> 正面
            cardBack.startAnimation(flipOut);
            cardFront.startAnimation(flipIn);
            cardBack.setVisibility(View.GONE);
            cardFront.setVisibility(View.VISIBLE);
            isShowingAnswer = false;

            // 显示正面时：隐藏记住/忘记按钮
            layoutRememberForget.setVisibility(View.GONE);
            btnShowAnswer.setText("翻转卡片");
        }
    }

    private void handleReviewResult(boolean remembered) {
        if (currentWord == null) return;

        if (remembered) {
            // 记住：直接更新数据库，进入下一个单词
            updateWordMastery(currentWord, true);
            showNextWord();
        } else {
            // 忘记：先加入忘记列表，稍后重练
            forgottenWords.add(currentWord);
            Toast.makeText(this, "已加入待重练列表", Toast.LENGTH_SHORT).show();

            // 更新数据库（降低掌握程度）
            updateWordMastery(currentWord, false);

            // 显示下一个单词
            showNextWord();
        }
    }

    private void updateWordMastery(Word word, boolean remembered) {
        disposables.add(
                wordRepository.updateWordMastery(word, remembered)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Log.d(TAG, "单词 " + word.getEnglishWord() + " 更新成功");
                                },
                                throwable -> {
                                    Log.e(TAG, "更新失败", throwable);
                                }
                        )
        );
    }

    private void speakWord(String word) {
        if (textToSpeech != null) {
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /**
     * 生成新的复习列表
     */
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

    private String getTodayDateString() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA);
        return sdf.format(new java.util.Date());
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
        // 检查是否还有未复习的单词
        disposables.add(
                wordRepository.getWordsToReview()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                words -> {
                                    int remainingCount = words.size();

                                    if (remainingCount > 0 && isNewGroup) {
                                        new AlertDialog.Builder(this)
                                                .setTitle("复习完成")
                                                .setMessage("您已完成本组复习！\n\n还有 " + remainingCount + " 个单词需要复习，是否继续？")
                                                .setPositiveButton("继续复习", (dialog, which) -> {
                                                    resetReviewList();
                                                    loadReviewWords();
                                                })
                                                .setNegativeButton("结束复习", (dialog, which) -> {
                                                    finish();
                                                })
                                                .setCancelable(false)
                                                .show();
                                    } else if (remainingCount > 0) {
                                        Toast.makeText(this,
                                                "本组复习完成！还有 " + remainingCount + " 个单词待复习",
                                                Toast.LENGTH_LONG).show();
                                        finish();
                                    } else {
                                        Toast.makeText(this, "恭喜！所有单词都已掌握！", Toast.LENGTH_LONG).show();
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