package com.example.schedule;

import android.content.Context;
import android.content.SyncContext;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.ItemKeyedDataSource;
import androidx.paging.PageKeyedDataSource;
import androidx.paging.PositionalDataSource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

class ScheduleDataSource extends ItemKeyedDataSource<Calendar, Day> {

    private final DBHelper dbHelper;
    private ArrayList<String> data;
    Context context;

    public ScheduleDataSource(Context context, ArrayList<String> data) {
        this.context = context;
        dbHelper = new DBHelper(context);
        this.data = data;
    }

    private ArrayList<Day> getDays(Calendar startDate, int daysCount){
        Calendar cal = startDate;
        SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy EEE", new Locale("ru", "RU"));
        String date = format1.format(cal.getTime());
        final ArrayList<Day> daysRange = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        for(int i = 0; i < daysCount; i++){
            Day day = new Day();
            day.setDate(outputFormat.format(cal.getTime()));
            String group;
            if(data.get(0).equals("Студент")){
                Cursor cursor;
                group = data.get(2);
                day.setGroup(group);
                String request = "SELECT DISTINCT file_id FROM Schedule WHERE date='" + date + "' AND group_name='" + group + "'";
                String nestedRequest = "SELECT id FROM (SELECT id, Max(strftime(substr(date,7,4)||'-'||substr(date,4,2)||'-'||substr(date,1,2))) FROM ScheduleFiles WHERE id IN (" + request + "))";
                String nestedRequest2 = "SELECT * FROM Schedule WHERE date='" + date + "' AND group_name='" + group + "' AND file_id=(" + nestedRequest + ")";
                cursor = db.rawQuery(nestedRequest2, null);
                while (cursor.moveToNext()) {
                    //1 lesson_name, 2 lesson_type, 3 date, 4 start_time, 5 finish_time, 6 group_name, 7 teacher, 8 place
                    day.addNewLesson();
                    day.addLastLessonNameInfo(cursor.getString(1), cursor.getString(2));
                    day.addLastLessonTimeInfo(cursor.getString(4), cursor.getString(5));
                    day.addLastLessonMetaInfo(cursor.getString(7), cursor.getString(8));
                }
                cursor.close();
            } else if (data.get(0).equals("Преподаватель")){
                Cursor cursor;
                String[] start_times = new String[]{"8.30", "10.15", "12.20", "14.05", "15.50", "17.35", "19.20"};
                group = data.get(1);
                day.setGroup(group);
                for(int j = 0; j < start_times.length; j++){
                    String request = "SELECT DISTINCT file_id FROM Schedule WHERE date='" + date + "' AND teacher LIKE '%" + group + "%' AND start_time='" + start_times[j] + "'";
                    String nestedRequest = "SELECT id FROM (SELECT id, Max(strftime(substr(date,7,4)||'-'||substr(date,4,2)||'-'||substr(date,1,2))) FROM ScheduleFiles WHERE id IN (" + request + "))";
                    String nestedRequest2 = "SELECT * FROM Schedule WHERE date='" + date + "' AND teacher LIKE '%" + group + "%' AND file_id=(" + nestedRequest + ") AND start_time='" + start_times[j] + "'";
                    cursor = db.rawQuery(nestedRequest2, null);
                    if (cursor.getCount() >= 1){
                        day.addNewLesson();
                        ArrayList<String> lessons = new ArrayList<>();
                        ArrayList<String> groups = new ArrayList<>();
                        ArrayList<String> places = new ArrayList<>();
                        ArrayList<String> lesson_types = new ArrayList<>();
                        while (cursor.moveToNext()) {
                            day.addLastLessonTimeInfo(cursor.getString(4), cursor.getString(5));
                            if (!lessons.contains(cursor.getString(1))){
                                lessons.add(cursor.getString(1));
                                groups.add(cursor.getString(6));
                                places.add(cursor.getString(8));
                                lesson_types.add(cursor.getString(2));
                            } else {
                                int idx = lessons.indexOf(cursor.getString(1));
                                groups.set(idx, groups.get(idx)+", "+cursor.getString(6));
                            }
                        }
                        String lessonsStr = "";
                        String groupsStr = "";
                        String placesStr = "";
                        String lesson_typesStr = "";
                        for (int k = 0; k < lessons.size(); k++) {
                            if (lessonsStr.equals("")){
                                lessonsStr += lessons.get(k);
                            } else {
                                lessonsStr += "; "+lessons.get(k);
                            }
                            if (groupsStr.equals("")){
                                groupsStr += groups.get(k);
                            } else {
                                groupsStr += "; "+groups.get(k);
                            }
                            if (placesStr.equals("")){
                                placesStr += places.get(k);
                            } else {
                                placesStr += "; "+places.get(k);
                            }
                            if (lesson_typesStr.equals("")){
                                lesson_typesStr += lesson_types.get(k);
                            } else {
                                lesson_typesStr += "; "+lesson_types.get(k);
                            }
                        }
                        //1 lesson_name, 2 lesson_type, 3 date, 4 start_time, 5 finish_time, 6 group_name, 7 teacher, 8 place
                        day.addLastLessonNameInfo(lessonsStr, lesson_typesStr);
                        day.addLastLessonMetaInfo(groupsStr, placesStr);
                    }
                    cursor.close();
                }
            } else {
                day.setGroup("");
                day.addNewLesson();
                day.addLastLessonTimeInfo("", "");
                day.addLastLessonNameInfo("Отображаемое расписание не настроено", "");
                day.addLastLessonMetaInfo("", "");
            }
            if(day.getLessonsNumber()==0){
                day.addNewLesson();
                day.addLastLessonTimeInfo("", "");
                day.addLastLessonNameInfo("На этот день нет расписания", "");
                day.addLastLessonMetaInfo("", "");
            }
            daysRange.add(day);
            cal.add(Calendar.DATE,1);
            date = format1.format(cal.getTime());
        }
        db.close();
        return daysRange;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Calendar> params, @NonNull LoadInitialCallback<Day> callback) {
        Calendar cal = Calendar.getInstance();
        if (params.requestedInitialKey != null){
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            cal = params.requestedInitialKey;
            Log.e("Log", format.format(cal.getTime()));
        }
        ArrayList<Day> daysRange = getDays(cal, params.requestedLoadSize);
        callback.onResult(daysRange);
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Calendar> params, @NonNull LoadCallback<Day> callback) {
        Calendar cal = params.key;
        cal.add(Calendar.DATE, -params.requestedLoadSize);
        ArrayList<Day> daysRange = getDays(cal,params.requestedLoadSize);
        callback.onResult(daysRange);
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Calendar> params, @NonNull LoadCallback<Day> callback) {
        Calendar cal = params.key;
        cal.add(Calendar.DATE, params.requestedLoadSize);
        ArrayList<Day> daysRange = getDays(cal,params.requestedLoadSize);
        callback.onResult(daysRange);
    }

    @NonNull
    @Override
    public Calendar getKey(@NonNull Day item) {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(format.parse(item.getDate()));
        } catch(ParseException e) {
            return null;
        }
        return cal;
    }
}
