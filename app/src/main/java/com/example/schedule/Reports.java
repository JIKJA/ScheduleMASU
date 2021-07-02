package com.example.schedule;

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.TransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

public class Reports extends AppCompatActivity implements ReportDialog.OnInputListener{
	RelativeLayout reportsProgressBar;
	private DBHelper dbHelper;
	SharedPreferences sharedPref;
	String teacher;
	RecyclerView recyclerView;
	ReportRecyclerAdapter adapter;
	LinearLayoutManager layoutManager;
	TableLayout table;
	ScrollView scrollView;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reports);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		reportsProgressBar = findViewById(R.id.reports_progressbar);
		dbHelper = new DBHelper(this);
		sharedPref = getSharedPreferences("com.example.schedule", MODE_PRIVATE);

		teacher = sharedPref.getString("teacher", "");
		if (teacher.equals("")){
			getSupportActionBar().setTitle("Формирование отчета");
		} else {
			getSupportActionBar().setTitle(teacher);
			DialogFragment dialog = ReportDialog.newInstance(getApplicationContext(), teacher);
			dialog.show(getFragmentManager(), "report_dialog");
		}

		recyclerView = findViewById(R.id.recyclerViewReport);
		table = findViewById(R.id.table);
		scrollView = findViewById(R.id.scroll_view);

	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void getReportsResult(String[] result) {
		if (result[0].equals("Расписание дисциплины за период")){
			// 0-report type, 1-start date, 2-end date, 3-discipline
			// create recyclerView
			progressBarSetVisible(true);
			ArrayList<Day> days = loadDays(result[1], result[2], result[3], teacher);
			layoutManager = new LinearLayoutManager(this);
			recyclerView.setLayoutManager(layoutManager);
			adapter = new ReportRecyclerAdapter(days, this);
			// Set CustomAdapter as the adapter for RecyclerView.
			recyclerView.setAdapter(adapter);
			progressBarSetVisible(false);
			recyclerView.setVisibility(View.VISIBLE);
		} else if(result[0].equals("Отчет о нагрузке за период")){
			// 0-report type, 1-start date, 2-end date
			// create table
			progressBarSetVisible(true);
			ArrayList<String> disciplines = new ArrayList<>();
			ArrayList<Integer[]> countables = new ArrayList<>();
			ArrayList<String> lessonTypes = getLessonTypes(result[1], result[2], teacher);
			countLessons(result[1], result[2], teacher, disciplines, countables, lessonTypes);

			// populate table with data
			int rowNumber = disciplines.size()+1;
			int columnNumber = lessonTypes.size()+1;
			TableRow.LayoutParams params= new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
			params.setMargins(2,2,2,2);
			for (int i=0; i < rowNumber; i++) {
				TableRow row = new TableRow(this);
				row.setLayoutParams(new
						TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
						TableRow.LayoutParams.WRAP_CONTENT));
				for (int j=0; j < columnNumber; j++) {
					TextView tv = new TextView(this);
					tv.setLayoutParams(params);
					tv.setBackgroundColor(Color.parseColor("#FFFFFF"));
					tv.setPadding(5,5,5,5);
					tv.setTextSize(16);
					tv.setTextColor(Color.parseColor("#000000"));
					tv.setGravity(Gravity.CENTER);
					tv.setMaxLines(1);
					if (i==0 && j==0){
						tv.setText("  ");
					} else if (i==0 && j>0){
						tv.setText(lessonTypes.get(j-1));
					} else if (i>0 && j==0){
						tv.setGravity(Gravity.LEFT);
						tv.setText(disciplines.get(i-1));
					} else {
						tv.setText(String.valueOf(countables.get(i-1)[j-1]));
					}
					row.addView(tv);
				}
				table.addView(row);
			}
			// "Itogo" row
			TableRow row = new TableRow(this);
			row.setLayoutParams(new
					TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT));
			for (int i=0; i < columnNumber; i++) {
				TextView tv = new TextView(this);
				tv.setLayoutParams(params);
				tv.setBackgroundColor(Color.parseColor("#FFFFFF"));
				tv.setPadding(5,5,5,5);
				tv.setTextSize(16);
				tv.setTextColor(Color.parseColor("#000000"));
				tv.setGravity(Gravity.CENTER);
				tv.setMaxLines(1);
				if (i==0){
					tv.setGravity(Gravity.LEFT);
					tv.setText("Итого:");
				} else {
					int count = 0;
					for (int j = 1; j < rowNumber; j++) {
						count+=countables.get(j-1)[i-1];
					}
					tv.setText(String.valueOf(count));
				}
				row.addView(tv);
			}
			table.addView(row);

			progressBarSetVisible(false);
			scrollView.setVisibility(View.VISIBLE);
		}
	}

	private ArrayList<Day> loadDays(String startDate, String endDate, String discipline, String teacher) {
		SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");
		SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy EEE", new Locale("ru", "RU"));

		Calendar startCal = Calendar.getInstance();
		Calendar endCal = Calendar.getInstance();
		try {
			startCal.setTime(format1.parse(startDate));
			endCal.setTime(format1.parse(endDate));
		} catch(ParseException e) {
			e.printStackTrace();
		}
		long diff = endCal.getTimeInMillis() - startCal.getTimeInMillis();
		int daysCount = (int) (diff / (24 * 60 * 60 * 1000));

		String date = startDate;
		final ArrayList<Day> daysRange = new ArrayList<>();
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		for(int i = 0; i < daysCount; i++){
			Day day = new Day();
			day.setDate(outputFormat.format(startCal.getTime()));
			Cursor cursor;
			String[] start_times = new String[]{"8.30", "10.15", "12.20", "14.05", "15.50", "17.35", "19.20"};
			day.setGroup(teacher);
			for(int j = 0; j < start_times.length; j++){
				String request = "SELECT DISTINCT file_id FROM Schedule WHERE date='" + date + "' AND teacher LIKE '%" + teacher + "%' AND start_time='" + start_times[j] + "' AND lesson_name LIKE '%" + discipline + "%'";
				String nestedRequest = "SELECT id FROM (SELECT id, Max(strftime(substr(date,7,4)||'-'||substr(date,4,2)||'-'||substr(date,1,2))) FROM ScheduleFiles WHERE id IN (" + request + "))";
				String nestedRequest2 = "SELECT * FROM Schedule WHERE date='" + date + "' AND teacher LIKE '%" + teacher + "%' AND file_id IN (" + nestedRequest + ") AND start_time='" + start_times[j] + "'AND lesson_name LIKE '%" + discipline + "%'";
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
			if(day.getLessonsNumber()!=0){
				daysRange.add(day);
			}
			startCal.add(Calendar.DATE,1);
			date = format1.format(startCal.getTime());
		}
		db.close();
		return daysRange;
	}

	private void progressBarSetVisible(boolean visibility){
		if(visibility){
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
			reportsProgressBar.setVisibility(RelativeLayout.VISIBLE);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
			reportsProgressBar.setVisibility(RelativeLayout.INVISIBLE);
		}
	}

	private ArrayList<String> getLessonTypes(String startDate, String endDate, String teacher) {
		SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");
		SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy EEE", new Locale("ru", "RU"));

		Calendar startCal = Calendar.getInstance();
		Calendar endCal = Calendar.getInstance();
		try {
			startCal.setTime(format1.parse(startDate));
			endCal.setTime(format1.parse(endDate));
		} catch(ParseException e) {
			e.printStackTrace();
		}
		long diff = endCal.getTimeInMillis() - startCal.getTimeInMillis();
		int daysCount = (int) (diff / (24 * 60 * 60 * 1000));

		String date = startDate;
		final ArrayList<String> lessonTypes = new ArrayList<>();
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		for(int i = 0; i < daysCount; i++){
			Cursor cursor;
			String[] start_times = new String[]{"8.30", "10.15", "12.20", "14.05", "15.50", "17.35", "19.20"};
			for(int j = 0; j < start_times.length; j++){
				String request = "SELECT DISTINCT file_id FROM Schedule WHERE date='" + date + "' AND teacher LIKE '%" + teacher + "%' AND start_time='" + start_times[j] + "'";
				String nestedRequest = "SELECT id FROM (SELECT id, Max(strftime(substr(date,7,4)||'-'||substr(date,4,2)||'-'||substr(date,1,2))) FROM ScheduleFiles WHERE id IN (" + request + "))";
				String nestedRequest2 = "SELECT * FROM Schedule WHERE date='" + date + "' AND teacher LIKE '%" + teacher + "%' AND file_id IN (" + nestedRequest + ") AND start_time='" + start_times[j] + "'";
				cursor = db.rawQuery(nestedRequest2, null);
				while (cursor.moveToNext()) {
					if (!lessonTypes.contains(cursor.getString(2))){
						lessonTypes.add(cursor.getString(2));
					}
				}
				cursor.close();
			}
			startCal.add(Calendar.DATE,1);
			date = format1.format(startCal.getTime());
		}
		db.close();
		return lessonTypes;
	}

	private void countLessons(String startDate, String endDate, String teacher, ArrayList<String> disciplines, ArrayList<Integer[]> countables, ArrayList<String> lessonTypes){
		SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");
		SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy EEE", new Locale("ru", "RU"));

		Calendar startCal = Calendar.getInstance();
		Calendar endCal = Calendar.getInstance();
		try {
			startCal.setTime(format1.parse(startDate));
			endCal.setTime(format1.parse(endDate));
		} catch(ParseException e) {
			e.printStackTrace();
		}
		long diff = endCal.getTimeInMillis() - startCal.getTimeInMillis();
		int daysCount = (int) (diff / (24 * 60 * 60 * 1000));

		String date = startDate;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		for(int i = 0; i < daysCount; i++){
			Cursor cursor;
			String[] start_times = new String[]{"8.30", "10.15", "12.20", "14.05", "15.50", "17.35", "19.20"};
			for(int j = 0; j < start_times.length; j++){
				String request = "SELECT DISTINCT file_id FROM Schedule WHERE date='" + date + "' AND teacher LIKE '%" + teacher + "%' AND start_time='" + start_times[j] + "'";
				String nestedRequest = "SELECT id FROM (SELECT id, Max(strftime(substr(date,7,4)||'-'||substr(date,4,2)||'-'||substr(date,1,2))) FROM ScheduleFiles WHERE id IN (" + request + "))";
				String nestedRequest2 = "SELECT * FROM Schedule WHERE date='" + date + "' AND teacher LIKE '%" + teacher + "%' AND file_id IN (" + nestedRequest + ") AND start_time='" + start_times[j] + "'";
				cursor = db.rawQuery(nestedRequest2, null);
				while (cursor.moveToNext()) {
					//1 lesson_name, 2 lesson_type, 3 date, 4 start_time, 5 finish_time, 6 group_name, 7 teacher, 8 place
					if (disciplines.contains(cursor.getString(1))){
						countables.get(disciplines.indexOf(cursor.getString(1)))[lessonTypes.indexOf(cursor.getString(2))]++;
					} else {
						disciplines.add(cursor.getString(1));
						Integer[] arr = new Integer[lessonTypes.size()];
						Arrays.fill(arr, 0);
						arr[lessonTypes.indexOf(cursor.getString(2))]++;
						countables.add(arr);
					}
				}
				cursor.close();
			}
			startCal.add(Calendar.DATE,1);
			date = format1.format(startCal.getTime());
		}
		db.close();
	}
}
