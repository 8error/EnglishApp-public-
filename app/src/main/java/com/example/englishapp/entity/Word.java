package com.example.englishapp.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "words")
public class Word {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "english_word")
    private String englishWord;

    @ColumnInfo(name = "chinese_meaning")
    private String chineseMeaning;

    private String phonetic; // 音标

    @ColumnInfo(name = "example_sentence")
    private String exampleSentence; // 例句

    @ColumnInfo(name = "example_translation")
    private String exampleTranslation; // 例句翻译

    @ColumnInfo(name = "mastery_level", defaultValue = "0")
    private int masteryLevel; // 掌握程度 0-5

    @ColumnInfo(name = "review_count", defaultValue = "0")
    private int reviewCount; // 复习次数

    @ColumnInfo(name = "last_review")
    private Date lastReview; // 上次复习时间

    @ColumnInfo(name = "next_review")
    private Date nextReview; // 下次复习时间

    @ColumnInfo(name = "create_time")
    private Date createTime; // 创建时间

    private String tags; // 标签（如：四级、考研、日常）

    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    private boolean isFavorite; // 是否收藏

    // 空构造函数（Room 需要）
    public Word() {
        this.createTime = new Date();
        this.lastReview = new Date();
        this.nextReview = new Date();
        this.masteryLevel = 0;
        this.reviewCount = 0;
        this.isFavorite = false;
    }

    // 带参数的构造函数（添加了 @Ignore 注解）
    @Ignore
    public Word(String englishWord, String chineseMeaning, String phonetic,
                String exampleSentence, String exampleTranslation, String tags) {
        this();
        this.englishWord = englishWord;
        this.chineseMeaning = chineseMeaning;
        this.phonetic = phonetic;
        this.exampleSentence = exampleSentence;
        this.exampleTranslation = exampleTranslation;
        this.tags = tags;
    }

    // Getter 和 Setter 方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEnglishWord() {
        return englishWord;
    }

    public void setEnglishWord(String englishWord) {
        this.englishWord = englishWord;
    }

    public String getChineseMeaning() {
        return chineseMeaning;
    }

    public void setChineseMeaning(String chineseMeaning) {
        this.chineseMeaning = chineseMeaning;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public String getExampleSentence() {
        return exampleSentence;
    }

    public void setExampleSentence(String exampleSentence) {
        this.exampleSentence = exampleSentence;
    }

    public String getExampleTranslation() {
        return exampleTranslation;
    }

    public void setExampleTranslation(String exampleTranslation) {
        this.exampleTranslation = exampleTranslation;
    }

    public int getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(int masteryLevel) {
        this.masteryLevel = masteryLevel;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Date getLastReview() {
        return lastReview;
    }

    public void setLastReview(Date lastReview) {
        this.lastReview = lastReview;
    }

    public Date getNextReview() {
        return nextReview;
    }

    public void setNextReview(Date nextReview) {
        this.nextReview = nextReview;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    // 实用方法
    public String getMasteryStars() {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            stars.append(i < masteryLevel ? "★" : "☆");
        }
        return stars.toString();
    }

    public boolean needsReview() {
        return nextReview == null || new Date().after(nextReview);
    }
}