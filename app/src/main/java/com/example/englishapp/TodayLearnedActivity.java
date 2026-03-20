package com.example.englishapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.englishapp.adapter.WordAdapter;
import com.example.englishapp.entity.Word;
import com.example.englishapp.repository.WordRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TodayLearnedActivity extends AppCompatActivity {

    private static final String TAG = "TodayLearnedActivity";

    private RecyclerView rvWords;
    private TextView tvEmpty;
    private Toolbar toolbar;
    private TextView tvDate;
    private TextView tvCount;

    private WordRepository wordRepository;
    private CompositeDisposable disposables = new CompositeDisposable();
    private WordAdapter wordAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today_learned);

        initViews();
        setupToolbar();
        setupRecyclerView();

        wordRepository = new WordRepository(getApplication());

        loadTodayLearnedWords();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvWords = findViewById(R.id.rv_words);
        tvEmpty = findViewById(R.id.tv_empty);
        tvDate = findViewById(R.id.tv_date);
        tvCount = findViewById(R.id.tv_count);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("今日学习记录");
        }

        // 显示当前日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
        tvDate.setText(sdf.format(new Date()));
    }

    private void setupRecyclerView() {
        rvWords.setLayoutManager(new LinearLayoutManager(this));
        wordAdapter = new WordAdapter();

        // 设置点击事件（可选）
        wordAdapter.setOnItemClickListener(this::onWordClick);

        // 设置收藏点击事件（可选）
        wordAdapter.setOnFavoriteClickListener((word, position) -> {
            // 今日学习记录页面可能不需要收藏功能，可以留空或简单提示
            // Toast.makeText(this, "收藏功能", Toast.LENGTH_SHORT).show();
        });

        rvWords.setAdapter(wordAdapter);
    }

    /**
     * 加载今日学习过的单词
     */
    private void loadTodayLearnedWords() {
        // 获取今日开始和结束的时间戳
        long startOfDay = getStartOfDayTimestamp();
        long endOfDay = startOfDay + 24 * 60 * 60 * 1000;

        disposables.add(
                wordRepository.getAllWords()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                allWords -> {
                                    List<Word> todayWords = new ArrayList<>();

                                    for (Word word : allWords) {
                                        // 检查创建时间或上次复习时间是否在今天
                                        long createTime = word.getCreateTime() != null ? word.getCreateTime().getTime() : 0;
                                        long lastReview = word.getLastReview() != null ? word.getLastReview().getTime() : 0;

                                        if ((createTime >= startOfDay && createTime <= endOfDay) ||
                                                (lastReview >= startOfDay && lastReview <= endOfDay)) {
                                            todayWords.add(word);
                                        }
                                    }

                                    // 按时间倒序排列（最新的在前）
                                    todayWords.sort((w1, w2) -> {
                                        long time1 = Math.max(
                                                w1.getLastReview() != null ? w1.getLastReview().getTime() : 0,
                                                w1.getCreateTime() != null ? w1.getCreateTime().getTime() : 0
                                        );
                                        long time2 = Math.max(
                                                w2.getLastReview() != null ? w2.getLastReview().getTime() : 0,
                                                w2.getCreateTime() != null ? w2.getCreateTime().getTime() : 0
                                        );
                                        return Long.compare(time2, time1);
                                    });

                                    // 更新UI
                                    if (todayWords.isEmpty()) {
                                        tvEmpty.setVisibility(View.VISIBLE);
                                        rvWords.setVisibility(View.GONE);
                                    } else {
                                        tvEmpty.setVisibility(View.GONE);
                                        rvWords.setVisibility(View.VISIBLE);
                                        wordAdapter.setWords(todayWords);
                                    }

                                    tvCount.setText("今日学习 " + todayWords.size() + " 个单词");
                                },
                                throwable -> {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    tvEmpty.setText("加载失败：" + throwable.getMessage());
                                }
                        )
        );
    }

    /**
     * 获取今天开始的时间戳
     */
    private long getStartOfDayTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void onWordClick(Word word) {
        // 可以跳转到单词详情页（后续实现）
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