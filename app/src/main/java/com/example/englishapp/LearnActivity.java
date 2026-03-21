package com.example.englishapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LearnActivity extends AppCompatActivity {

    private static final String TAG = "LearnActivity";
    private static final int MODE_LEARN = 1;      // 学习模式
    private static final int MODE_TEST = 2;       // 测试模式

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
    private Button btnKnown;
    private Button btnUnknown;
    private LinearLayout layoutKnownButtons;
    private ImageView btnSpeak;            // 正面语音按钮
    private ImageView btnSpeakBack;        // 背面语音按钮
    private ProgressBar progressBar;
    private Toolbar toolbar;

    // 测试模式特有的视图
    private CardView cardTest;
    private TextView tvTestMeaning;
    private EditText etTestInput;
    private Button btnTestSubmit;
    private TextView tvTestResult;
    private LinearLayout layoutLearnButtons;
    private LinearLayout layoutTestButtons;

    private WordRepository wordRepository;
    private CompositeDisposable disposables = new CompositeDisposable();

    private List<Word> learnWords = new ArrayList<>();  // 学习的单词
    private List<Word> testWords = new ArrayList<>();   // 测试的单词
    private int currentIndex = 0;
    private int dailyLearnCount = 10;
    private int currentMode = MODE_LEARN;  // 当前模式

    private boolean isShowingAnswer = false;
    private TextToSpeech textToSpeech;
    private Animation flipIn;
    private Animation flipOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);

        // 获取每日学习数量设置
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        dailyLearnCount = prefs.getInt("daily_learn_count", 10);

        initViews();
        setupToolbar();
        initAnimations();
        initTextToSpeech();

        wordRepository = new WordRepository(getApplication());

        loadNewWords();
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
        btnKnown = findViewById(R.id.btn_known);
        btnUnknown = findViewById(R.id.btn_unknown);
        layoutKnownButtons = findViewById(R.id.layout_known_buttons);
        btnSpeak = findViewById(R.id.btn_speak);
        btnSpeakBack = findViewById(R.id.btn_speak_back);
        progressBar = findViewById(R.id.progress_bar);

        // 测试模式视图
        cardTest = findViewById(R.id.card_test);
        tvTestMeaning = findViewById(R.id.tv_test_meaning);
        etTestInput = findViewById(R.id.et_test_input);
        btnTestSubmit = findViewById(R.id.btn_test_submit);
        tvTestResult = findViewById(R.id.tv_test_result);
        layoutLearnButtons = findViewById(R.id.layout_learn_buttons);
        layoutTestButtons = findViewById(R.id.layout_test_buttons);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("学习新单词");
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

    /**
     * 加载未学习的单词
     */
    private void loadNewWords() {
        progressBar.setVisibility(View.VISIBLE);

        disposables.add(
                wordRepository.getAllWords()
                        .firstOrError()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                allWords -> {
                                    // 过滤出未学习的单词
                                    List<Word> newWords = new ArrayList<>();
                                    for (Word word : allWords) {
                                        if (word.getMasteryLevel() == 0 && word.getReviewCount() == 0) {
                                            newWords.add(word);
                                        }
                                    }

                                    // 先打乱所有未学习单词的顺序
                                    Collections.shuffle(newWords);

                                    // 然后再取前 dailyLearnCount 个
                                    int count = Math.min(newWords.size(), dailyLearnCount);
                                    learnWords = new ArrayList<>(newWords.subList(0, count));

                                    progressBar.setVisibility(View.GONE);

                                    if (learnWords.isEmpty()) {
                                        Toast.makeText(this, "没有未学习的单词", Toast.LENGTH_LONG).show();
                                        finish();
                                    } else {
                                        // 进入学习模式
                                        enterLearnMode();
                                    }
                                },
                                throwable -> {
                                    progressBar.setVisibility(View.GONE);
                                    Log.e(TAG, "加载单词失败", throwable);
                                    Toast.makeText(this, "加载失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                        )
        );
    }

    /**
     * 进入学习模式
     */
    private void enterLearnMode() {
        currentMode = MODE_LEARN;
        currentIndex = 0;

        // 隐藏测试模式视图
        cardTest.setVisibility(View.GONE);
        layoutTestButtons.setVisibility(View.GONE);

        // 显示学习模式视图
        cardFront.setVisibility(View.VISIBLE);
        cardBack.setVisibility(View.GONE);
        layoutLearnButtons.setVisibility(View.VISIBLE);
        layoutKnownButtons.setVisibility(View.GONE);
        btnShowAnswer.setVisibility(View.VISIBLE);

        // 更新标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("学习新单词");
        }

        updateProgress();
        showCurrentWord();
    }

    /**
     * 进入测试模式
     */
    private void enterTestMode() {
        currentMode = MODE_TEST;

        // 复制学习列表到测试列表，并再次打乱顺序
        testWords = new ArrayList<>(learnWords);
        Collections.shuffle(testWords);
        currentIndex = 0;

        // 隐藏学习模式视图
        cardFront.setVisibility(View.GONE);
        cardBack.setVisibility(View.GONE);
        layoutLearnButtons.setVisibility(View.GONE);

        // 显示测试模式视图
        cardTest.setVisibility(View.VISIBLE);
        layoutTestButtons.setVisibility(View.VISIBLE);

        // 更新标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("默写测试");
        }

        showNextTest();
    }

    /**
     * 显示当前单词（学习模式）
     */
    private void showCurrentWord() {
        if (currentIndex >= learnWords.size()) {
            // 学习完成，进入测试模式
            enterTestMode();
            return;
        }

        Word currentWord = learnWords.get(currentIndex);

        // 更新正面卡片
        tvWord.setText(currentWord.getEnglishWord());
        if (tvWordBack != null) {
            tvWordBack.setText(currentWord.getEnglishWord());
        }
        tvPhonetic.setText("/" + (currentWord.getPhonetic() != null ? currentWord.getPhonetic() : "") + "/");
        tvMeaning.setText(currentWord.getChineseMeaning());
        tvExample.setText(currentWord.getExampleSentence() != null ? currentWord.getExampleSentence() : "");
        tvExampleTranslation.setText(currentWord.getExampleTranslation() != null ? currentWord.getExampleTranslation() : "");

        // 确保显示正面
        cardFront.setVisibility(View.VISIBLE);
        cardBack.setVisibility(View.GONE);
        isShowingAnswer = false;

        layoutKnownButtons.setVisibility(View.GONE);
        btnShowAnswer.setText("显示答案");
        btnShowAnswer.setEnabled(true);

        // 自动朗读
        speakWord(currentWord.getEnglishWord());
    }

    /**
     * 显示下一个测试单词
     */
    private void showNextTest() {
        if (currentIndex >= testWords.size()) {
            // 所有测试完成
            finishLearning();
            return;
        }

        Word currentWord = testWords.get(currentIndex);

        // 显示中文释义
        tvTestMeaning.setText(currentWord.getChineseMeaning());

        // 清空输入框
        etTestInput.setText("");
        tvTestResult.setVisibility(View.GONE);

        // 更新进度
        tvProgress.setText(String.format("测试 %d / %d", currentIndex + 1, testWords.size()));
    }

    /**
     * 更新进度（学习模式）
     */
    private void updateProgress() {
        tvProgress.setText(String.format("学习 %d / %d", currentIndex + 1, learnWords.size()));
    }

    private void setupClickListeners() {
        // 显示答案按钮
        btnShowAnswer.setOnClickListener(v -> flipCard());

        // 认识/不认识按钮
        btnKnown.setOnClickListener(v -> handleLearningResult(true));
        btnUnknown.setOnClickListener(v -> handleLearningResult(false));

        // 正面语音按钮
        if (btnSpeak != null) {
            btnSpeak.setOnClickListener(v -> {
                if (currentMode == MODE_LEARN && currentIndex < learnWords.size()) {
                    speakWord(learnWords.get(currentIndex).getEnglishWord());
                } else if (currentMode == MODE_TEST && currentIndex < testWords.size()) {
                    speakWord(testWords.get(currentIndex).getEnglishWord());
                }
            });
        }

        // 背面语音按钮
        if (btnSpeakBack != null) {
            btnSpeakBack.setOnClickListener(v -> {
                if (currentMode == MODE_LEARN && currentIndex < learnWords.size()) {
                    speakWord(learnWords.get(currentIndex).getEnglishWord());
                }
            });
        }

        // 卡片点击翻转
        cardFront.setOnClickListener(v -> flipCard());
        cardBack.setOnClickListener(v -> flipCard());

        // 测试模式按钮
        btnTestSubmit.setOnClickListener(v -> checkTestAnswer());

        // 输入框实时监听
        etTestInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvTestResult.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void flipCard() {
        if (cardFront.getVisibility() == View.VISIBLE) {
            // 正面 -> 背面
            cardFront.startAnimation(flipOut);
            cardBack.startAnimation(flipIn);
            cardFront.setVisibility(View.GONE);
            cardBack.setVisibility(View.VISIBLE);
            isShowingAnswer = true;

            layoutKnownButtons.setVisibility(View.VISIBLE);
            btnShowAnswer.setText("返回正面");
        } else {
            // 背面 -> 正面
            cardBack.startAnimation(flipOut);
            cardFront.startAnimation(flipIn);
            cardBack.setVisibility(View.GONE);
            cardFront.setVisibility(View.VISIBLE);
            isShowingAnswer = false;

            layoutKnownButtons.setVisibility(View.GONE);
            btnShowAnswer.setText("显示答案");
        }
    }

    /**
     * 检查测试答案
     */
    private void checkTestAnswer() {
        if (currentIndex >= testWords.size()) return;

        Word currentWord = testWords.get(currentIndex);
        String userAnswer = etTestInput.getText().toString().trim();
        String correctAnswer = currentWord.getEnglishWord();

        // 忽略大小写进行比较
        if (userAnswer.equalsIgnoreCase(correctAnswer)) {
            // 回答正确
            tvTestResult.setText("✓ 正确！");
            tvTestResult.setTextColor(getColor(android.R.color.holo_green_dark));
            tvTestResult.setBackgroundColor(getColor(android.R.color.transparent));
            tvTestResult.setVisibility(View.VISIBLE);
            tvTestResult.setPadding(0, 0, 0, 0);

            // 延迟进入下一个
            new Handler().postDelayed(() -> {
                currentIndex++;
                showNextTest();
            }, 800);
        } else {
            // 回答错误
            tvTestResult.setText("✗ 错误！正确答案是: " + correctAnswer);
            tvTestResult.setTextColor(getColor(android.R.color.white));
            tvTestResult.setBackgroundColor(getColor(android.R.color.holo_red_dark));
            tvTestResult.setVisibility(View.VISIBLE);
            tvTestResult.setPadding(16, 12, 16, 12);

            // 输入框错误提示
            etTestInput.setError("正确答案: " + correctAnswer);

            // 朗读正确答案
            speakWord(correctAnswer);

            // 短暂显示正确答案在卡片上
            String originalMeaning = tvTestMeaning.getText().toString();
            tvTestMeaning.setText(correctAnswer);
            tvTestMeaning.setTextColor(getColor(android.R.color.holo_red_dark));

            // 2秒后恢复
            new Handler().postDelayed(() -> {
                tvTestMeaning.setText(originalMeaning);
                tvTestMeaning.setTextColor(getColor(R.color.teal_700));
            }, 2000);

            // 清空输入框，让用户重新输入
            etTestInput.setText("");
            etTestInput.requestFocus();
        }
    }

    /**
     * 处理学习结果
     */
    private void handleLearningResult(boolean known) {
        if (currentIndex >= learnWords.size()) return;

        Word currentWord = learnWords.get(currentIndex);

        if (known) {
            // 认识：设置 masteryLevel = 1，下次复习时间为30分钟后
            currentWord.setMasteryLevel(1);
            currentWord.setReviewCount(1);
            currentWord.setLastReview(new Date());

            long nextReviewTime = System.currentTimeMillis() + 30 * 60 * 1000;
            currentWord.setNextReview(new Date(nextReviewTime));

            disposables.add(
                    wordRepository.update(currentWord)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> {
                                        Toast.makeText(this, "已标记为认识", Toast.LENGTH_SHORT).show();
                                        moveToNextWord();
                                    },
                                    throwable -> {
                                        Log.e(TAG, "更新失败", throwable);
                                        Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show();
                                    }
                            )
            );
        } else {
            // 不认识：保持 masteryLevel = 0，但设置5分钟后复习
            currentWord.setReviewCount(currentWord.getReviewCount() + 1);
            currentWord.setLastReview(new Date());

            long nextReviewTime = System.currentTimeMillis() + 5 * 60 * 1000;
            currentWord.setNextReview(new Date(nextReviewTime));

            disposables.add(
                    wordRepository.update(currentWord)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> {
                                        Toast.makeText(this, "已标记为不认识，5分钟后再次出现", Toast.LENGTH_SHORT).show();
                                        moveToNextWord();
                                    },
                                    throwable -> {
                                        Log.e(TAG, "更新失败", throwable);
                                        Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show();
                                    }
                            )
            );
        }
    }

    /**
     * 移动到下一个单词（学习模式）
     */
    private void moveToNextWord() {
        currentIndex++;
        if (currentIndex < learnWords.size()) {
            updateProgress();
            showCurrentWord();
        } else {
            // 学习完成，进入测试模式
            enterTestMode();
        }
    }

    /**
     * 学习完成
     */
    private void finishLearning() {
        new AlertDialog.Builder(this)
                .setTitle("恭喜！")
                .setMessage("您已完成今天的学习和测试！")
                .setPositiveButton("完成", (dialog, which) -> {
                    finish();
                })
                .show();
    }

    private void speakWord(String word) {
        if (textToSpeech != null && word != null && !word.isEmpty()) {
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
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