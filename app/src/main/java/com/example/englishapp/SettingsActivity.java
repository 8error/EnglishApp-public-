package com.example.englishapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private EditText etDailyReviewCount;
    private RadioGroup rgReviewStrategy;
    private RadioButton rbStrict;
    private RadioButton rbFlexible;
    private RadioGroup rgReviewOrder;
    private RadioButton rbOrderNormal;
    private RadioButton rbOrderRandom;
    private Spinner spinnerWordBook;  // 单词本选择下拉框
    private CheckBox cbAutoNext;
    private Button btnSaveSettings;
    private Toolbar toolbar;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupToolbar();
        setupWordBookSpinner();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etDailyReviewCount = findViewById(R.id.et_daily_review_count);
        rgReviewStrategy = findViewById(R.id.rg_review_strategy);
        rbStrict = findViewById(R.id.rb_strict);
        rbFlexible = findViewById(R.id.rb_flexible);
        rgReviewOrder = findViewById(R.id.rg_review_order);
        rbOrderNormal = findViewById(R.id.rb_order_normal);
        rbOrderRandom = findViewById(R.id.rb_order_random);
        spinnerWordBook = findViewById(R.id.spinner_word_book);
        cbAutoNext = findViewById(R.id.cb_auto_next);
        btnSaveSettings = findViewById(R.id.btn_save_settings);

        sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("复习设置");
        }
    }

    /**
     * 设置单词本选择下拉框 - 只保留四级和六级
     */
    private void setupWordBookSpinner() {
        // 只保留四级词汇和六级词汇选项
        String[] wordBooks = {"四级词汇", "六级词汇"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wordBooks);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWordBook.setAdapter(adapter);
    }

    private void loadSettings() {
        // 加载保存的设置
        int dailyCount = sharedPreferences.getInt("daily_review_count", 20);
        boolean isStrict = sharedPreferences.getBoolean("strict_mode", true);
        boolean autoNext = sharedPreferences.getBoolean("auto_next", false);
        boolean isRandomOrder = sharedPreferences.getBoolean("random_order", true);
        String selectedWordBook = sharedPreferences.getString("word_book", "四级词汇");

        etDailyReviewCount.setText(String.valueOf(dailyCount));

        // 复习策略
        if (isStrict) {
            rbStrict.setChecked(true);
        } else {
            rbFlexible.setChecked(true);
        }

        // 复习顺序
        if (isRandomOrder) {
            rbOrderRandom.setChecked(true);
        } else {
            rbOrderNormal.setChecked(true);
        }

        // 单词本选择
        int spinnerPosition = getSpinnerPosition(selectedWordBook);
        spinnerWordBook.setSelection(spinnerPosition);

        cbAutoNext.setChecked(autoNext);
    }

    /**
     * 获取单词本在Spinner中的位置
     */
    private int getSpinnerPosition(String wordBook) {
        String[] wordBooks = {"四级词汇", "六级词汇"};
        for (int i = 0; i < wordBooks.length; i++) {
            if (wordBooks[i].equals(wordBook)) {
                return i;
            }
        }
        return 0; // 默认返回第一个（四级词汇）
    }

    private void setupListeners() {
        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        // 获取输入值
        String countStr = etDailyReviewCount.getText().toString().trim();
        if (countStr.isEmpty()) {
            Toast.makeText(this, "请输入每日复习数量", Toast.LENGTH_SHORT).show();
            return;
        }

        int dailyCount;
        try {
            dailyCount = Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dailyCount <= 0) {
            Toast.makeText(this, "复习数量必须大于0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dailyCount > 500) {
            Toast.makeText(this, "复习数量不能超过500", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isStrict = rbStrict.isChecked();
        boolean autoNext = cbAutoNext.isChecked();
        boolean isRandomOrder = rbOrderRandom.isChecked();
        String selectedWordBook = spinnerWordBook.getSelectedItem().toString();

        // 保存到SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("daily_review_count", dailyCount);
        editor.putBoolean("strict_mode", isStrict);
        editor.putBoolean("auto_next", autoNext);
        editor.putBoolean("random_order", isRandomOrder);
        editor.putString("word_book", selectedWordBook);
        editor.apply();

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 获取每日复习数量
     */
    public static int getDailyReviewCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        return prefs.getInt("daily_review_count", 20);
    }

    /**
     * 获取是否严格模式
     */
    public static boolean isStrictMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        return prefs.getBoolean("strict_mode", true);
    }

    /**
     * 获取是否自动进入下一个单词
     */
    public static boolean isAutoNext(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        return prefs.getBoolean("auto_next", false);
    }

    /**
     * 获取是否随机顺序
     */
    public static boolean isRandomOrder(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        return prefs.getBoolean("random_order", true);
    }

    /**
     * 获取选中的单词本
     */
    public static String getSelectedWordBook(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        return prefs.getString("word_book", "四级词汇");
    }

    /**
     * 根据单词本名称获取对应的标签查询条件
     */
    public static String getWordBookTag(String wordBook) {
        switch (wordBook) {
            case "四级词汇":
                return "四级";
            case "六级词汇":
                return "六级";
            default:
                return null; // 默认返回所有（实际上只有四级和六级）
        }
    }
}