package com.example.englishapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.englishapp.adapter.WordAdapter;
import com.example.englishapp.entity.Word;
import com.example.englishapp.repository.WordRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREF_FIRST_LAUNCH = "first_launch";

    private TextView tvTotalWords;      // 总单词
    private TextView tvNeedReview;       // 待复习
    private TextView tvMastered;         // 已掌握
    private ImageView ivSettings;
    private MaterialButton btnLearn;     // 学习按钮
    private MaterialButton btnReview;    // 复习按钮
    private MaterialButton btnAdd;       // 添加单词按钮

    private WordRepository wordRepository;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图
        initViews();

        // 初始化 Repository
        wordRepository = new WordRepository(getApplication());

        // 设置点击事件
        setupClickListeners();

        // 检查是否首次启动
        checkFirstLaunch();

        // 加载统计数据
        loadStats();

        // 设置实时数据观察
        setupDataObservers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到页面时刷新数据
        refreshStats();
    }

    private void initViews() {
        tvTotalWords = findViewById(R.id.tv_total_words);
        tvNeedReview = findViewById(R.id.tv_need_review);
        tvMastered = findViewById(R.id.tv_mastered);
        ivSettings = findViewById(R.id.iv_settings);
        btnLearn = findViewById(R.id.btn_learn);
        btnReview = findViewById(R.id.btn_review);
        btnAdd = findViewById(R.id.btn_add);
    }

    private void setupClickListeners() {
        ivSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        btnLearn.setOnClickListener(v -> {
            // 学习新单词功能（待实现）
            Toast.makeText(this, "学习功能开发中", Toast.LENGTH_SHORT).show();
        });

        btnReview.setOnClickListener(v -> {
            // 检查是否有待复习单词
            String needReviewStr = tvNeedReview.getText().toString();
            int needReviewCount = Integer.parseInt(needReviewStr);

            if (needReviewCount == 0) {
                // 今日没有待复习单词，询问是否学习新词
                showNoReviewDialog();
            } else {
                // 进入复习界面
                startReviewActivity();
            }
        });

        btnAdd.setOnClickListener(v -> {
            startAddWordActivity();
        });
    }

    /**
     * 显示没有复习单词时的对话框
     */
    private void showNoReviewDialog() {
        new AlertDialog.Builder(this)
                .setTitle("今日无复习任务")
                .setMessage("今天没有需要复习的单词，是否要学习一些新单词？")
                .setPositiveButton("学习新词", (dialog, which) -> {
                    // 进入学习界面（待实现）
                    Toast.makeText(this, "学习功能开发中", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 检查是否首次启动
     */
    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(PREF_FIRST_LAUNCH, true);

        if (isFirstLaunch) {
            showFirstLaunchDialog();
            prefs.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply();
        }
    }

    /**
     * 显示首次启动设置对话框
     */
    private void showFirstLaunchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("欢迎使用英语单词本");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        TextView message = new TextView(this);
        message.setText("请设置您每天想学习的单词数量：");
        message.setTextSize(16);
        layout.addView(message);

        EditText input = new EditText(this);
        input.setHint("例如：10");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText("10");
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("开始学习", (dialog, which) -> {
            String value = input.getText().toString().trim();
            int dailyCount;
            try {
                dailyCount = Integer.parseInt(value);
                if (dailyCount <= 0) dailyCount = 10;
                if (dailyCount > 100) dailyCount = 100;
            } catch (NumberFormatException e) {
                dailyCount = 10;
            }

            SharedPreferences.Editor editor = getSharedPreferences("AppSettings", MODE_PRIVATE).edit();
            editor.putInt("daily_learn_count", dailyCount);
            editor.apply();

            Toast.makeText(this, "已设置每天学习 " + dailyCount + " 个新单词", Toast.LENGTH_SHORT).show();
        });

        builder.setCancelable(false);
        builder.show();
    }

    /**
     * 加载统计数据
     */
    private void loadStats() {
        disposables.add(
                wordRepository.getAllWords()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::updateStats,
                                throwable -> {
                                    Log.e(TAG, "加载失败", throwable);
                                }
                        )
        );
    }

    /**
     * 设置数据实时观察
     */
    private void setupDataObservers() {
        disposables.add(
                wordRepository.getAllWords()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::updateStats,
                                throwable -> {
                                    Log.e(TAG, "观察数据失败", throwable);
                                }
                        )
        );
    }

    /**
     * 更新统计数据
     */
    private void updateStats(List<Word> words) {
        // 总单词数
        tvTotalWords.setText(String.valueOf(words.size()));

        // 计算待复习单词（基于艾宾浩斯遗忘曲线）
        long now = System.currentTimeMillis();
        int needReviewCount = 0;
        int masteredCount = 0;

        for (Word word : words) {
            // 根据 next_review 判断是否需要复习
            if (isWordNeedReview(word, now)) {
                needReviewCount++;
            }
            if (word.getMasteryLevel() >= 4) {
                masteredCount++;
            }
        }

        tvNeedReview.setText(String.valueOf(needReviewCount));
        tvMastered.setText(String.valueOf(masteredCount));

        Log.d(TAG, "统计更新 - 总单词: " + words.size() +
                ", 待复习: " + needReviewCount +
                ", 已掌握: " + masteredCount);
    }

    /**
     * 判断单词是否需要复习
     */
    private boolean isWordNeedReview(Word word, long currentTime) {
        Object nextReview = word.getNextReview();
        if (nextReview == null) return false;

        if (nextReview instanceof Long) {
            return (Long) nextReview <= currentTime;
        } else if (nextReview instanceof Date) {
            return ((Date) nextReview).getTime() <= currentTime;
        } else if (nextReview instanceof Integer) {
            return ((Integer) nextReview) * 1000L <= currentTime;
        }
        return false;
    }

    /**
     * 刷新统计数据
     */
    private void refreshStats() {
        disposables.add(
                wordRepository.getAllWords()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::updateStats,
                                throwable -> {
                                    Log.e(TAG, "刷新统计失败", throwable);
                                }
                        )
        );
    }

    private void startReviewActivity() {
        Intent intent = new Intent(this, ReviewActivity.class);
        startActivity(intent);
    }

    private void startAddWordActivity() {
        Intent intent = new Intent(this, AddWordActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle("关于")
                    .setMessage("英语背单词App\n版本 1.0\n\n基于艾宾浩斯遗忘曲线的高效记忆工具")
                    .setPositiveButton("确定", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}