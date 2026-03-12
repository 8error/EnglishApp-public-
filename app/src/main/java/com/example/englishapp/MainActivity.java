package com.example.englishapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private TextView tvTotalWords;
    private TextView tvTodayReview;
    private TextView tvMastered;
    private RecyclerView rvWords;
    private LinearLayout chipGroupTags;  // 改为 LinearLayout 以匹配布局文件
    private Chip chipAll;
    private ImageView ivSearch;
    private Toolbar toolbar;
    private MaterialButton btnReview;
    private MaterialButton btnAdd;

    private WordRepository wordRepository;
    private CompositeDisposable disposables = new CompositeDisposable();
    private WordAdapter wordAdapter;
    private List<Word> allWords = new ArrayList<>();
    private List<Word> currentWords = new ArrayList<>();
    private String currentTag = "全部";
    private String currentKeyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图
        initViews();

        // 设置工具栏
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("英语单词本");
        }

        // 初始化 Repository
        wordRepository = new WordRepository(getApplication());

        // 设置 RecyclerView
        setupRecyclerView();

        // 设置点击事件
        setupClickListeners();

        // 加载数据
        loadData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvTotalWords = findViewById(R.id.tv_total_words);
        tvTodayReview = findViewById(R.id.tv_today_review);
        tvMastered = findViewById(R.id.tv_mastered);
        rvWords = findViewById(R.id.rv_words);
        chipGroupTags = findViewById(R.id.layout_tags);  // LinearLayout 类型
        chipAll = findViewById(R.id.chip_all);
        ivSearch = findViewById(R.id.iv_search);
        btnReview = findViewById(R.id.btn_review);
        btnAdd = findViewById(R.id.btn_add);
    }

    private void setupRecyclerView() {
        rvWords.setLayoutManager(new LinearLayoutManager(this));
        wordAdapter = new WordAdapter();
        wordAdapter.setOnItemClickListener(this::onWordClick);
        wordAdapter.setOnFavoriteClickListener(this::onFavoriteClick);
        rvWords.setAdapter(wordAdapter);
    }

    private void setupClickListeners() {
        // 搜索按钮
        ivSearch.setOnClickListener(v -> {
            Toast.makeText(this, "搜索功能开发中", Toast.LENGTH_SHORT).show();
        });

        // 开始复习按钮
        btnReview.setOnClickListener(v -> {
            if (currentWords.isEmpty()) {
                Toast.makeText(this, "没有单词可复习", Toast.LENGTH_SHORT).show();
                return;
            }
            startReviewActivity();
        });

        // 添加单词按钮
        btnAdd.setOnClickListener(v -> {
            startAddWordActivity();
        });

        // 全部标签点击
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTag = "全部";
                filterWords();
            }
        });
    }

    private void loadData() {
        disposables.add(
                wordRepository.getAllWords()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::onWordsLoaded,
                                throwable -> {
                                    Toast.makeText(this, "加载失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                        )
        );

        // 加载今日复习数量
        disposables.add(
                wordRepository.getTodayReviewCount()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                count -> tvTodayReview.setText(String.valueOf(count)),
                                throwable -> tvTodayReview.setText("0")
                        )
        );
    }

    private void onWordsLoaded(List<Word> words) {
        allWords = words;
        currentWords = new ArrayList<>(words);

        // 更新统计
        updateStats(words);

        // 更新标签
        updateTags(words);

        // 更新列表
        wordAdapter.setWords(words);
    }

    private void updateStats(List<Word> words) {
        // 总单词数
        tvTotalWords.setText(String.valueOf(words.size()));

        // 已掌握单词（掌握程度 >= 4）
        long masteredCount = 0;
        for (Word word : words) {
            if (word.getMasteryLevel() >= 4) {
                masteredCount++;
            }
        }
        tvMastered.setText(String.valueOf(masteredCount));
    }

    private void updateTags(List<Word> words) {
        // 收集所有标签
        Set<String> tagSet = new HashSet<>();
        for (Word word : words) {
            String tags = word.getTags();
            if (tags != null && !tags.isEmpty()) {
                String[] tagArray = tags.split(",");
                for (String tag : tagArray) {
                    tagSet.add(tag.trim());
                }
            }
        }

        // 移除除了"全部"以外的所有标签
        while (chipGroupTags.getChildCount() > 1) {
            chipGroupTags.removeViewAt(1);
        }

        // 添加新标签
        for (String tag : tagSet) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentTag = tag;
                    filterWords();
                }
            });
            chipGroupTags.addView(chip);
        }
    }

    private void filterWords() {
        if (currentTag.equals("全部")) {
            currentWords = new ArrayList<>(allWords);
        } else {
            currentWords = new ArrayList<>();
            for (Word word : allWords) {
                String tags = word.getTags();
                if (tags != null && tags.contains(currentTag)) {
                    currentWords.add(word);
                }
            }
        }

        // 如果有搜索关键词，进一步过滤
        if (!currentKeyword.isEmpty()) {
            List<Word> filtered = new ArrayList<>();
            String keyword = currentKeyword.toLowerCase();
            for (Word word : currentWords) {
                if (word.getEnglishWord().toLowerCase().contains(keyword) ||
                        word.getChineseMeaning().contains(keyword)) {
                    filtered.add(word);
                }
            }
            currentWords = filtered;
        }

        wordAdapter.setWords(currentWords);
    }

    private void onWordClick(Word word) {
        // 点击单词，显示详情
        Toast.makeText(this, word.getEnglishWord() + ": " + word.getChineseMeaning(), Toast.LENGTH_SHORT).show();
    }

    private void onFavoriteClick(Word word, int position) {
        // 更新收藏状态
        word.setFavorite(!word.isFavorite());

        disposables.add(
                wordRepository.update(word)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    wordAdapter.notifyItemChanged(position);
                                    String message = word.isFavorite() ? "已添加到收藏" : "已取消收藏";
                                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                },
                                throwable -> {
                                    Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show();
                                    // 恢复原状态
                                    word.setFavorite(!word.isFavorite());
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

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("搜索单词...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentKeyword = query;
                filterWords();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentKeyword = newText;
                filterWords();
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_favorite) {
            // 显示收藏的单词
            currentTag = "收藏";
            filterWords();
            return true;
        } else if (itemId == R.id.action_settings) {
            Toast.makeText(this, "设置开发中", Toast.LENGTH_SHORT).show();
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