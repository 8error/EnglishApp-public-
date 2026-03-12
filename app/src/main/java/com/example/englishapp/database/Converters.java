package com.example.englishapp.database;

import androidx.room.TypeConverter;

import java.util.Date;

public class Converters {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static String[] fromString(String value) {
        return value == null ? new String[0] : value.split(",");
    }

    @TypeConverter
    public static String arrayToString(String[] array) {
        if (array == null || array.length == 0) {
            return "";
        }
        return String.join(",", array);
    }
}