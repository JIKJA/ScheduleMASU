package com.example.schedule;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "ScheduleDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table Schedule ("
                + "id integer primary key,"
                + "lesson_name text,"
                + "lesson_type text,"
                + "date text,"
                + "start_time text,"
                + "finish_time,"
                + "group_name text,"
                + "teacher text,"
                + "place text,"
                + "file_id text" + ");");
        db.execSQL("create table ScheduleFiles ("
                + "id integer primary key autoincrement,"
                + "date text,"
                + "size integer,"
                + "file_name text,"
                + "faculty text" + ");");
        db.execSQL("CREATE INDEX idx_date ON Schedule(date);");
        db.execSQL("CREATE INDEX idx_group ON Schedule(group_name);");
        db.execSQL("CREATE INDEX idx_teacher ON Schedule(teacher);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
