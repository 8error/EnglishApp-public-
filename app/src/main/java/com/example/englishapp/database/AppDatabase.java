package com.example.englishapp.database;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.englishapp.dao.WordDao;
import com.example.englishapp.entity.Word;

@Database(entities = {Word.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract WordDao wordDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                        AppDatabase.class, "english_word_database")
                               .createFromAsset("words.db")
                                .fallbackToDestructiveMigration()
                                .build();
                    } catch (Exception e) {
                        Log.e("AppDatabase", "数据库创建失败", e);
                        throw e;
                    }
                }
            }
        }
        return INSTANCE;
    }
}