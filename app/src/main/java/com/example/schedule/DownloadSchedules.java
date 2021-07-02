package com.example.schedule;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadSchedules{
    private Context context;
    private String LOG_TAG = "Log";
    SAXparser parser;
    DBHelper dbHelper;
    SQLiteDatabase db;
    ArrayList<Day> parsingData;

    DownloadSchedules(Context context) {
        parsingData = new ArrayList<>();
        this.context = context;
        dbHelper = new DBHelper(context);
    }

    boolean proceedSchedule(ArrayList<String> data){
        boolean invalidateflag = false;
        try {
            // prepare teachers list from MASU site
            ArrayList<String> teachers = new ArrayList<>();
            getTeachersFromSite(teachers);
            // load needed faculties
            // if student then load only needed faculty
            ArrayList<String> facultyUrl = new ArrayList<>();
            ArrayList<String> facultyNames = new ArrayList<>();
            Document doc = Jsoup.connect("https://www.masu.edu.ru/student/timetable/").get();
            Elements rows = doc.select("h1+ul li");
            if(data.get(0).equals("Студент")) {
                String fac = data.get(1);
                for (Element row : rows) {
                    if(row.text().equals(fac)){
                        facultyUrl.add(row.child(0).attr("href"));
                        facultyNames.add(row.text());
                    }
                }
            // if teacher then load all faculties
            } else if(data.get(0).equals("Преподаватель")){
                for (Element row : rows) {
                    if(!row.text().toLowerCase().contains("колледж")) { // exclude college
                        facultyUrl.add(row.child(0).attr("href"));
                        facultyNames.add(row.text());
                    }
                }
            }

            // load xlsx from url
            for(int i = 0; i < facultyUrl.size(); i++) {
                // find href to schedule file on MASU site
                doc = Jsoup.connect("https://www.masu.edu.ru/student/timetable/" + facultyUrl.get(i)).get();
                rows = doc.select("tbody tr");
                for (Element row : rows) {
                    String content = row.text();
                    if (content.equals("Очная форма обучения")){//||content.equals("Заочная форма обучения")||content.equals("Очно-заочная форма обучения")){
                        continue;
                    } else if (row.id().startsWith("bx")) {
                        String downloadUrl = row.child(0).child(0).attr("abs:href");
                        String downloadFileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1); // name on server
                        String fileName = row.child(0).child(0).text(); // name on MASU site
                        String date = row.child(2).text(); // dd.MM.yy HH:mm
                        downloadUrl = downloadUrl.substring(0, downloadUrl.lastIndexOf('/') + 1) + URLEncoder.encode(downloadFileName, "UTF-8").replace("+", "%20");
                        if (downloadUrl.endsWith(".xlsx")) {
                            URL url = new URL(downloadUrl);
                            int size = getSize(url);
                            if(data.get(0).equals("Студент")) {
                                if (!checkIfExists(fileName, size, date, facultyNames.get(i), data.get(2))) {
                                    File file = load(url);
                                    parser = new SAXparser(file, teachers);
                                    parser.parseExcel(parsingData, data.get(2));
                                }
                            } else if(data.get(0).equals("Преподаватель")){
                                File file = load(url);
                                parser = new SAXparser(file, teachers);
                                ArrayList<String> groups = new ArrayList<>();
                                parser.getGroups(groups);
                                for (String group: groups){
                                    if (!checkIfExists(fileName, size, date, facultyNames.get(i), group)) {
                                        parser.parseExcel(parsingData, group);
                                    }
                                }
                            }
                            if(parsingData.size()>0){
                                invalidateflag = true;
                                insertInDb(fileName, size, date, facultyNames.get(i));
                                parsingData.clear();
                            }
                        }
                    } else {
                        break;
                    }
                }
            }
            return invalidateflag;
        } catch(IOException e){
            Log.e(LOG_TAG, "Connection to MASU exception: " + e.getMessage());
            return invalidateflag;
        }
    }

    File load (URL url){
        File cacheFile = new File(context.getFilesDir(), "currentXLSX.xlsx");

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); //Open Url Connection
            conn.setRequestMethod("GET");
            conn.connect();

            InputStream is = conn.getInputStream(); // download and write to cache
            FileOutputStream os = new FileOutputStream(cacheFile);
            byte[] buff = new byte[4096];
            int bytes = is.read(buff);
            while (bytes != -1) {
                os.write(buff, 0, bytes);
                bytes = is.read(buff);
            }
            is.close();
            os.close();
            conn.disconnect();
        } catch(IOException e){
            Log.e(LOG_TAG, "Download Error Exception " + e.getMessage());
        }
        return cacheFile;
    }

    int getSize (URL url){
        int size = 0;
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); //Open Url Connection
            conn.setRequestMethod("GET");
            conn.connect();
            size = conn.getContentLength();
            conn.disconnect();
        } catch(IOException e){
            Log.e(LOG_TAG, "Download Error Exception " + e.getMessage());
        }
        return size;
    }

    boolean checkIfExists(String fileName, int size, String date, String faculty, String group){
        boolean existion = false;

        db = dbHelper.getWritableDatabase();
        String request = "SELECT DISTINCT group_name FROM Schedule WHERE group_name = '" + group + "' AND file_id IN (SELECT id FROM ScheduleFiles WHERE file_name = '" + fileName + "' AND date = '" + date + "' AND size = " + size + " AND faculty = '" + faculty +"')";
        Cursor cursor = db.rawQuery(request, null);
        if(cursor.getCount()>0){
            existion = true;
        }
        cursor.close();
        db.close();
        return existion;
    }

    void insertInDb(String fileName, int size, String date, String faculty){

        db = dbHelper.getWritableDatabase();
        String request = "SELECT id FROM ScheduleFiles WHERE file_name = '" + fileName + "' AND date = '" + date + "' AND size = " + size + " AND faculty = '" + faculty +"'";
        Cursor cursor = db.rawQuery(request, null);
        int id;
        if(cursor.getCount() > 0){
            cursor.moveToFirst();
            id = cursor.getInt(0);
        } else {
            ContentValues cv = new ContentValues();
            cv.put("file_name", fileName);
            cv.put("date", date);
            cv.put("size", size);
            cv.put("faculty", faculty);
            id = (int)db.insert("ScheduleFiles", null, cv);
        }
        db.beginTransaction();
        try {
            for (int i = 0; i < parsingData.size(); i++){
                String dayDate = parsingData.get(i).getDate();
                String group = parsingData.get(i).getGroup();
                for (int j = 0; j < parsingData.get(i).getLessonsNumber(); j++){
                    Day.Lesson lesson = parsingData.get(i).getLesson(j);
                    ContentValues cv = new ContentValues();
                    cv.put("date", dayDate);
                    cv.put("group_name", group);
                    cv.put("lesson_name", lesson.lesson_name);
                    cv.put("lesson_type", lesson.lesson_type);
                    cv.put("start_time", lesson.start_time);
                    cv.put("finish_time", lesson.finish_time);
                    cv.put("teacher", lesson.teacher);
                    cv.put("place", lesson.place);
                    cv.put("file_id", id);
                    db.insert("Schedule", null, cv);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    void getFaculties(ArrayList<String> filesData){
        try {
            Document doc = Jsoup.connect("https://www.masu.edu.ru/student/timetable/").get();
            Elements rows = doc.select("h1+ul li");
            for (Element row : rows) {
                if(!row.text().toLowerCase().contains("колледж")) {
                    filesData.add(row.text());
                }
            }
        } catch(IOException e){
            // if connection error then load faculties from db
            db = dbHelper.getWritableDatabase();
            Cursor cursor = db.rawQuery("SELECT DISTINCT faculty FROM ScheduleFiles", null);
            while(cursor.moveToNext()) {
                filesData.add(cursor.getString(0));
            }
            cursor.close();
            db.close();
            Log.e("ERROR", "Connection to MASU exception: " + e.getMessage());
        }
    }

    void getGroups(ArrayList<String> filesData, String faculty){
        try {
            String faculty_href = "";
            // find href to schedule file on MASU site
            Document doc = Jsoup.connect("https://www.masu.edu.ru/student/timetable/").get();
            Elements rows = doc.select("h1+ul li");
            for (Element row : rows) {
                if(row.text().equals(faculty)){
                    faculty_href = row.child(0).attr("href");
                }
            }
            if(faculty_href.equals("")) return;
            doc = Jsoup.connect("https://www.masu.edu.ru/student/timetable/"+faculty_href).get();
            rows = doc.select("tbody tr");
            String study_form;
            for (Element row : rows) {
                String content = row.text();
                if(content.equals("Очная форма обучения")){//||content.equals("Заочная форма обучения")||content.equals("Очно-заочная форма обучения")){
                    study_form = content.split(" ")[0];
                    continue;
                } else if(row.id().startsWith("bx")){
                    String downloadUrl = row.child(0).child(0).attr("abs:href");
                    String downloadFileName = downloadUrl.substring(downloadUrl.lastIndexOf('/')+1); // name of file at MASU server
                    String fileName = row.child(0).child(0).text(); // name of file at MASU site
                    String date = row.child(2).text();
                    date = date.substring(0, date.lastIndexOf(" ")); // date of last change
                    downloadUrl = downloadUrl.substring(0,downloadUrl.lastIndexOf('/')+1)+URLEncoder.encode(downloadFileName, "UTF-8").replace("+","%20");
                    if(downloadUrl.endsWith(".xlsx")) {
                        File file = load(new URL(downloadUrl));
                        parser = new SAXparser(file, new ArrayList<String>());
                        try {
                            parser.getGroups(filesData);
                        }
                        catch (IOException e){
                            Log.e("ERR", e.getMessage());
                        }
                    }
                } else {
                    break;
                }
            }
        } catch(IOException e){
            db = dbHelper.getWritableDatabase();
            Cursor cursor = db.rawQuery("SELECT DISTINCT id FROM ScheduleFiles WHERE faculty=?", new String[]{faculty});
            String request = "SELECT DISTINCT group_name FROM Schedule WHERE file_id IN (";
            cursor.moveToNext();
            request += "" + cursor.getInt(0);
            while(cursor.moveToNext()) {
                request += ", " + cursor.getInt(0);
            }
            request += ")";
            cursor = db.rawQuery(request, null);
            while(cursor.moveToNext()) {
                filesData.add(cursor.getString(0));
            }
            cursor.close();
            db.close();
            Log.e(LOG_TAG, "Connection to MASU exception: " + e.getMessage());
        }
    }

    void getTeachers(ArrayList<String> filesData){
        db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT teacher FROM Schedule", null);
        while(cursor.moveToNext()){
            String teacher = cursor.getString(0);
            if (teacher.indexOf(", ")==-1){
                if (!filesData.contains(teacher)) {
                    filesData.add(teacher);
                }
            } else {
                String[] teachers = teacher.split(", ");
                for (String element: teachers) {
                    if (!filesData.contains(element)) {
                        filesData.add(element);
                    }
                }
            }
        }
        cursor.close();
        db.close();
    }

    static void getTeachersFromSite(ArrayList<String> teachers){
        ArrayList<String> kafsUrls = new ArrayList<>();
        Document doc;
        try {
            doc = Jsoup.connect("https://www.masu.edu.ru/structure/kafs/").get();
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }
        Elements rows = doc.select(".anker li");
        for (Element row : rows) {
            kafsUrls.add(row.child(0).attr("href"));
        }
        for(int i = 0; i < kafsUrls.size()-1; i++){
            try {
                doc = Jsoup.connect("https://www.masu.edu.ru"+kafsUrls.get(i)+"prepods/").get();
            } catch(IOException e) {
                e.printStackTrace();
            }
            rows = doc.select(".mt0");
            for (Element row : rows) {
                String[] teacherParts = row.text().split(" ");
                String teacher = teacherParts[0] +" "+ teacherParts[1].substring(0,1) +"."+ teacherParts[2].substring(0,1);
                if (!teachers.contains(teacher)){
                    teachers.add(teacher);
                }
            }
        }
    }
}
