package com.example.englishapp.dao;

import androidx.room.ColumnInfo;  // 添加这个 import
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.englishapp.entity.Word;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface WordDao {

    @Insert
    Completable insert(Word word);

    @Insert
    Completable insertAll(List<Word> words);

    @Update
    Completable update(Word word);

    @Delete
    Completable delete(Word word);

    @Query("SELECT * FROM words ORDER BY next_review ASC")
    Flowable<List<Word>> getAllWords();

    @Query("SELECT * FROM words WHERE id = :id")
    Single<Word> getWordById(int id);

    @Query("SELECT * FROM words WHERE next_review < :currentTime OR next_review IS NULL ORDER BY next_review ASC")
    Flowable<List<Word>> getWordsToReview(long currentTime);

    @Query("SELECT * FROM words WHERE is_favorite = 1 ORDER BY create_time DESC")
    Flowable<List<Word>> getFavoriteWords();

    @Query("SELECT * FROM words WHERE tags LIKE '%' || :tag || '%'")
    Flowable<List<Word>> getWordsByTag(String tag);

    @Query("SELECT * FROM words WHERE english_word LIKE '%' || :keyword || '%' OR chinese_meaning LIKE '%' || :keyword || '%'")
    Flowable<List<Word>> searchWords(String keyword);

    @Query("SELECT mastery_level, COUNT(*) as count FROM words GROUP BY mastery_level")
    Flowable<List<MasteryStats>> getMasteryStats();

    @Query("SELECT COUNT(*) FROM words WHERE date(next_review/1000, 'unixepoch') = date('now')")
    Single<Integer> getTodayReviewCount();

    @Query("UPDATE words SET mastery_level = 0, next_review = :currentTime WHERE mastery_level > 0")
    Completable resetAllReviews(long currentTime);

    // 修改后的静态内部类
    static class MasteryStats {
        @ColumnInfo(name = "mastery_level")  // 指定对应的数据库列名
        public int masteryLevel;

        public int count;  // 这个字段名已经匹配
    }
}