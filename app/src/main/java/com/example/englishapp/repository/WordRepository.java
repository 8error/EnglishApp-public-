package com.example.englishapp.repository;

import android.app.Application;

import com.example.englishapp.dao.WordDao;
import com.example.englishapp.database.AppDatabase;
import com.example.englishapp.entity.Word;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public class WordRepository {

    private final WordDao wordDao;
    private final ExecutorService executorService;

    public WordRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        wordDao = database.wordDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    // 插入单词
    public Completable insert(Word word) {
        return wordDao.insert(word);
    }

    // 批量插入
    public Completable insertAll(List<Word> words) {
        return wordDao.insertAll(words);
    }

    // 更新单词
    public Completable update(Word word) {
        return wordDao.update(word);
    }

    // 删除单词
    public Completable delete(Word word) {
        return wordDao.delete(word);
    }

    // 获取所有单词
    public Flowable<List<Word>> getAllWords() {
        return wordDao.getAllWords();
    }

    // 根据ID获取单词
    public Single<Word> getWordById(int id) {
        return wordDao.getWordById(id);
    }

    // 获取需要复习的单词
    public Flowable<List<Word>> getWordsToReview() {
        return wordDao.getWordsToReview(System.currentTimeMillis());
    }

    // 搜索单词
    public Flowable<List<Word>> searchWords(String keyword) {
        return wordDao.searchWords(keyword);
    }

    // 更新单词掌握程度（根据艾宾浩斯算法）
    public Completable updateWordMastery(Word word, boolean remembered) {
        return Completable.fromAction(() -> {
            int newMasteryLevel;

            if (remembered) {
                // 回答正确，掌握程度+1（最多5）
                newMasteryLevel = Math.min(word.getMasteryLevel() + 1, 5);
            } else {
                // 回答错误，掌握程度-1（最少0）
                newMasteryLevel = Math.max(word.getMasteryLevel() - 1, 0);
            }

            word.setMasteryLevel(newMasteryLevel);
            word.setReviewCount(word.getReviewCount() + 1);
            word.setLastReview(new Date());

            // 根据艾宾浩斯算法计算下次复习时间
            long nextReviewTime = calculateNextReviewTime(newMasteryLevel);
            word.setNextReview(new Date(System.currentTimeMillis() + nextReviewTime));

            wordDao.update(word).blockingAwait();
        });
    }

    // 艾宾浩斯复习间隔（单位：毫秒）
    private long calculateNextReviewTime(int masteryLevel) {
        switch (masteryLevel) {
            case 0: return 5 * 60 * 1000L;        // 5分钟
            case 1: return 30 * 60 * 1000L;       // 30分钟
            case 2: return 12 * 60 * 60 * 1000L;  // 12小时
            case 3: return 24 * 60 * 60 * 1000L;  // 1天
            case 4: return 4 * 24 * 60 * 60 * 1000L; // 4天
            case 5: return 7 * 24 * 60 * 60 * 1000L; // 1周
            default: return 24 * 60 * 60 * 1000L;    // 默认1天
        }
    }

    // 获取今日复习数量
    public Single<Integer> getTodayReviewCount() {
        return wordDao.getTodayReviewCount();
    }

    // 注意：addSampleData() 方法已被移除，因为现在使用预填充数据库
}