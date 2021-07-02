package com.example.schedule;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ReportDialog extends DialogFragment {

	public OnInputListener mOnInputListener;
	Spinner reportSpinner, disciplineSpinner;
	TextView tvReport, tvDates, tvDiscipline;
	Button startDateBtn, endDateBtn;
	ArrayList<String> reports, disciplines;
	ArrayAdapter<String> reportSpinnerAdapter, disciplineSpinnerAdapter;
	ImageButton btn;
	RelativeLayout reportDialogProgressBar;
	Context context;
	String startDate, endDate;
	private DBHelper dbHelper;
	String teacher;

	public interface OnInputListener{
		void getReportsResult(String[] result);
	}

	static ReportDialog newInstance(Context context, String teacher){
		ReportDialog dialog = new ReportDialog();
		dialog.setData(context, teacher);
		return dialog;
	}

	void setData(Context context, String teacher){
		this.context = context;
		this.teacher = teacher;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_Schedule_FullScreenDialog);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.reports_dialog, container, false);
		tvReport = view.findViewById(R.id.tv_report);
		tvDates = view.findViewById(R.id.tv_dates);
		tvDiscipline = view.findViewById(R.id.tv_discipline);

		reportSpinner = (Spinner) view.findViewById(R.id.spinner_report);
		disciplineSpinner = (Spinner) view.findViewById(R.id.spinner_discipline);

		startDateBtn = view.findViewById(R.id.start_date_btn);
		endDateBtn = view.findViewById(R.id.end_date_btn);
		btn = view.findViewById(R.id.reports_dialog_close);

		reportDialogProgressBar = view.findViewById(R.id.report_dialog_progressbar);

		dbHelper = new DBHelper(context);
		startDate = "";
		endDate = "";

		return view;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().finish();
				dismiss();
			}
		});

		startDateBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Calendar dateAndTime = Calendar.getInstance();
				new DatePickerDialog(getDialog().getContext(), R.style.Theme_Schedule_DatePickerDialog, startDateListener,
						dateAndTime.get(Calendar.YEAR),
						dateAndTime.get(Calendar.MONTH),
						dateAndTime.get(Calendar.DAY_OF_MONTH))
						.show();
			}
		});

		endDateBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Calendar dateAndTime = Calendar.getInstance();
				new DatePickerDialog(getDialog().getContext(), R.style.Theme_Schedule_DatePickerDialog, endDateListener,
						dateAndTime.get(Calendar.YEAR),
						dateAndTime.get(Calendar.MONTH),
						dateAndTime.get(Calendar.DAY_OF_MONTH))
						.show();
			}
		});

		reports = new ArrayList<>();
		reports.add("Не выбрано");
		reports.add("Расписание дисциплины за период");
		reports.add("Отчет о нагрузке за период");
		reportSpinnerAdapter = new ArrayAdapter<String>(context, R.layout.spinner_item, reports);
		reportSpinnerAdapter.setDropDownViewResource(R.layout.spinner_item);
		reportSpinner.setAdapter(reportSpinnerAdapter);

		reportSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					tvDates.setVisibility(View.GONE);
					tvDiscipline.setVisibility(View.GONE);
					disciplineSpinner.setVisibility(View.GONE);
					startDateBtn.setVisibility(View.GONE);
					endDateBtn.setVisibility(View.GONE);
					startDate="";
					endDate="";
				} else if(position == 1){
					// discipline schedule over period is selected
					// select dates
					tvDates.setVisibility(View.VISIBLE);
					startDateBtn.setVisibility(View.VISIBLE);
					endDateBtn.setVisibility(View.VISIBLE);
					tvDiscipline.setVisibility(View.GONE);
					disciplineSpinner.setVisibility(View.GONE);
					startDate="";
					endDate="";
				} else if(position == 2){
					// teacher's workload is selected
					// select dates
					tvDates.setVisibility(View.VISIBLE);
					startDateBtn.setVisibility(View.VISIBLE);
					endDateBtn.setVisibility(View.VISIBLE);
					tvDiscipline.setVisibility(View.GONE);
					disciplineSpinner.setVisibility(View.GONE);
					startDate="";
					endDate="";
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		disciplineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0){

				} else {
					String[] result = {reportSpinner.getSelectedItem().toString(), startDate, endDate, parent.getSelectedItem().toString()};
					mOnInputListener.getReportsResult(result);
					dismiss();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	DatePickerDialog.OnDateSetListener startDateListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");
			Calendar date = Calendar.getInstance();
			date.set(Calendar.YEAR, year);
			date.set(Calendar.MONTH, monthOfYear);
			date.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			startDate = format1.format(date.getTime());
			if (!endDate.equals("")){
				datePickerSolver();
			}

		}
	};

	DatePickerDialog.OnDateSetListener endDateListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");
			Calendar date = Calendar.getInstance();
			date.set(Calendar.YEAR, year);
			date.set(Calendar.MONTH, monthOfYear);
			date.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			endDate = format1.format(date.getTime());
			if (!startDate.equals("")){
				datePickerSolver();
			}
		}
	};

	private void datePickerSolver(){
		if(reportSpinner.getSelectedItemId()==1){
			// load disciplines
			GetDisciplines task = new GetDisciplines();
			task.execute(teacher);
		} else if(reportSpinner.getSelectedItemId()==2){
			// send result
			String[] result = {reportSpinner.getSelectedItem().toString(), startDate, endDate};
			mOnInputListener.getReportsResult(result);
			dismiss();
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try{
			mOnInputListener = (OnInputListener) getActivity();
		} catch(ClassCastException e){
			Log.e("Error", "onAttach: ClassCastException "+e.getMessage());
		}
	}

	private void progressBarSetVisible(boolean visibility){
		if(visibility){
			getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
			reportDialogProgressBar.setVisibility(RelativeLayout.VISIBLE);
		} else {
			getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
			reportDialogProgressBar.setVisibility(RelativeLayout.INVISIBLE);
		}
	}

	class GetDisciplines extends AsyncTask<String, Void, ArrayList<String>> {
		int error = 0;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressBarSetVisible(true);
		}

		@Override
		protected ArrayList<String> doInBackground(String... teachers) {
			SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");
			ArrayList<String> disciplines = new ArrayList<>();
			SQLiteDatabase db = dbHelper.getWritableDatabase();

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
			if (daysCount < 0){
				error = 1;
			}
			Log.e("Log", ""+daysCount);
			String date = format1.format(startCal.getTime());
			String[] start_times = new String[]{"8.30", "10.15", "12.20", "14.05", "15.50", "17.35", "19.20"};
			for (int i = 0; i <= daysCount; i++) {
				for (String startTime: start_times) {
					String nestedRequest2 = "SELECT DISTINCT file_id FROM Schedule WHERE date='" + date + "' AND teacher LIKE '%" + teacher + "%' AND start_time='" + startTime + "'";
					String nestedRequest = "SELECT id FROM (SELECT id, Max(strftime(substr(date,7,4)||'-'||substr(date,4,2)||'-'||substr(date,1,2))) FROM ScheduleFiles WHERE id IN (" + nestedRequest2 + "))";
					String request = "SELECT DISTINCT lesson_name FROM Schedule WHERE date='" + date + "'AND teacher LIKE '%" + teachers[0] + "%' AND file_id IN (" + nestedRequest + ") AND start_time='" + startTime + "'";
					Cursor cursor;
					cursor = db.rawQuery(request, null);
					while (cursor.moveToNext()) {
						if (!disciplines.contains(cursor.getString(0))) {
							disciplines.add(cursor.getString(0));
						}
					}
					cursor.close();
				}
				startCal.add(Calendar.DATE,1);
				date = format1.format(startCal.getTime());
			}
			db.close();
			Collections.sort(disciplines);
			disciplines.add(0,"Не выбрано");
			return disciplines;
		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {
			super.onPostExecute(null);
			if (error == 1){
				Toast toast = Toast.makeText(context, "Дата начала должна быть раньше даты конца!", Toast.LENGTH_LONG);
				toast.show();
			}
			tvDiscipline.setVisibility(View.VISIBLE);
			disciplineSpinnerAdapter = new ArrayAdapter<String>(context, R.layout.spinner_item, result);
			disciplineSpinnerAdapter.setDropDownViewResource(R.layout.spinner_item);
			disciplineSpinner.setAdapter(disciplineSpinnerAdapter);
			disciplineSpinner.setVisibility(View.VISIBLE);
			progressBarSetVisible(false);
		}
	}
}

