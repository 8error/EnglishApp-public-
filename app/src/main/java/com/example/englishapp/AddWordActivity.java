package com.example.englishapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.englishapp.entity.Word;
import com.example.englishapp.repository.WordRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AddWordActivity extends AppCompatActivity {

    private EditText etWord;
    private EditText etPhonetic;
    private EditText etMeaning;
    private EditText etExample;
    private EditText etExampleTranslation;
    private ChipGroup chipGroupTags;
    private Button btnSave;
    private Toolbar toolbar;

    private WordRepository wordRepository;
    private CompositeDisposable disposables = new CompositeDisposable();

    // 预定义的标签
    private final List<String> predefinedTags = Arrays.asList("四级", "六级", "考研", "托福", "雅思", "GRE", "日常", "商务", "学术", "情感", "高级");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_word);

        // 初始化视图
        initViews();

        // 设置工具栏
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("添加新单词");
        }

        // 初始化 Repository
        wordRepository = new WordRepository(getApplication());

        // 设置标签
        setupTags();

        // 设置保存按钮点击事件
        btnSave.setOnClickListener(v -> saveWord());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etWord = findViewById(R.id.et_word);
        etPhonetic = findViewById(R.id.et_phonetic);
        etMeaning = findViewById(R.id.et_meaning);
        etExample = findViewById(R.id.et_example);
        etExampleTranslation = findViewById(R.id.et_example_translation);
        chipGroupTags = findViewById(R.id.chip_group_tags);
        btnSave = findViewById(R.id.btn_save);
    }

    private void setupTags() {
        for (String tag : predefinedTags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chipGroupTags.addView(chip);
        }
    }

    private void saveWord() {
        String word = etWord.getText().toString().trim();
        String phonetic = etPhonetic.getText().toString().trim();
        String meaning = etMeaning.getText().toString().trim();
        String example = etExample.getText().toString().trim();
        String exampleTranslation = etExampleTranslation.getText().toString().trim();

        // 验证必填字段
        if (TextUtils.isEmpty(word)) {
            etWord.setError("请输入单词");
            etWord.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(meaning)) {
            etMeaning.setError("请输入中文释义");
            etMeaning.requestFocus();
            return;
        }

        // 获取选中的标签
        List<String> selectedTags = new ArrayList<>();
        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupTags.getChildAt(i);
            if (chip.isChecked()) {
                selectedTags.add(chip.getText().toString());
            }
        }

        String tags = TextUtils.join(",", selectedTags);

        // 创建新单词
        Word newWord = new Word(word, meaning, phonetic, example, exampleTranslation, tags);

        // 保存到数据库
        disposables.add(
                wordRepository.insert(newWord)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Toast.makeText(this, "单词添加成功", Toast.LENGTH_SHORT).show();
                                    finish(); // 关闭当前页面
                                },
                                throwable -> {
                                    Toast.makeText(this, "添加失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                        )
        );
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
    }
}