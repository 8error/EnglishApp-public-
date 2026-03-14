package com.example.englishapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private EditText etDailyReviewCount;
    private RadioGroup rgReviewStrategy;
    private RadioButton rbStrict;
    private RadioButton rbFlexible;
    private RadioGroup rgReviewOrder;      // 新增：复习顺序选项组
    private RadioButton rbOrderNormal;     // 新增：默认顺序
    private RadioButton rbOrderRandom;     // 新增：随机顺序
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
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etDailyReviewCount = findViewById(R.id.et_daily_review_count);
        rgReviewStrategy = findViewById(R.id.rg_review_strategy);
        rbStrict = findViewById(R.id.rb_strict);
        rbFlexible = findViewById(R.id.rb_flexible);

        // 新增：初始化复习顺序相关视图
        rgReviewOrder = findViewById(R.id.rg_review_order);
        rbOrderNormal = findViewById(R.id.rb_order_normal);
        rbOrderRandom = findViewById(R.id.rb_order_random);

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

    private void loadSettings() {
        // 加载保存的设置，如果没有则使用默认值
        int dailyCount = sharedPreferences.getInt("daily_review_count", 20);
        boolean isStrict = sharedPreferences.getBoolean("strict_mode", true);
        boolean autoNext = sharedPreferences.getBoolean("auto_next", false);
        boolean isRandomOrder = sharedPreferences.getBoolean("random_order", true); // 新增：默认随机顺序

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

        cbAutoNext.setChecked(autoNext);
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
        boolean isRandomOrder = rbOrderRandom.isChecked(); // 新增：获取复习顺序设置

        // 保存到SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("daily_review_count", dailyCount);
        editor.putBoolean("strict_mode", isStrict);
        editor.putBoolean("auto_next", autoNext);
        editor.putBoolean("random_order", isRandomOrder); // 新增：保存复习顺序
        editor.apply();

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish(); // 返回上一页
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
     * 获取每日复习数量（静态方法，方便其他地方调用）
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
     * 新增：获取是否随机顺序
     */
    public static boolean isRandomOrder(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        return prefs.getBoolean("random_order", true); // 默认true，让新手体验到随机的好处
    }
}