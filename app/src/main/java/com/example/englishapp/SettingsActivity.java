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

        etDailyReviewCount.setText(String.valueOf(dailyCount));

        if (isStrict) {
            rbStrict.setChecked(true);
        } else {
            rbFlexible.setChecked(true);
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

        // 保存到SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("daily_review_count", dailyCount);
        editor.putBoolean("strict_mode", isStrict);
        editor.putBoolean("auto_next", autoNext);
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
}