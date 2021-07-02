package com.example.schedule;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

public class FullScreenDialog extends DialogFragment {

	public OnInputListener mOnInputListener;
	Spinner type_spinner, faculty_spinner, group_spinner, teacher_spinner;
	TextView facultyText, teacherText, groupText;
	ArrayList<String> types, faculties, groups, teachers;
	ArrayAdapter<String> type_adapter, faculty_adapter, group_adapter, teacher_adapter;
	ImageButton btn;
	RelativeLayout selectionProgressBar;
	Context context;

	public interface OnInputListener{
		void getSelectionResult(String[] result);
	}

	static FullScreenDialog newInstance(Context context){
		FullScreenDialog dialog = new FullScreenDialog();
		dialog.setContext(context);
		return dialog;
	}

	void setContext(Context context){
		this.context = context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_Schedule_FullScreenDialog);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.selection_dialog, container, false);
		facultyText = view.findViewById(R.id.textViewFaculty);
		teacherText = view.findViewById(R.id.textViewTeacher);
		groupText = view.findViewById(R.id.textViewGroup);

		type_spinner = (Spinner) view.findViewById(R.id.user_type);
		faculty_spinner = (Spinner) view.findViewById(R.id.select_faculty);
		group_spinner = (Spinner) view.findViewById(R.id.select_group);
		teacher_spinner = (Spinner) view.findViewById(R.id.select_teacher);

		btn = view.findViewById(R.id.fullscreen_dialog_close);

		selectionProgressBar = view.findViewById(R.id.selection_progressbar);

		return view;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		types = new ArrayList<>();
		types.add("Не выбрано");
		types.add("Студент");
		types.add("Преподаватель");
		type_adapter = new ArrayAdapter<String>(context, R.layout.spinner_item, types);
		type_adapter.setDropDownViewResource(R.layout.spinner_item);
		type_spinner.setAdapter(type_adapter);

		type_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					facultyText.setVisibility(View.GONE);
					teacherText.setVisibility(View.GONE);
					groupText.setVisibility(View.GONE);
					faculty_spinner.setVisibility(View.GONE);
					group_spinner.setVisibility(View.GONE);
					teacher_spinner.setVisibility(View.GONE);
				} else if(position == 1){
					// student selected
					// load faculty names
					GetFaculties task = new GetFaculties();
					task.execute();
				} else {
					// teacher selected
					GetTeachers task = new GetTeachers();
					task.execute();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		faculty_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0){
					group_spinner.setVisibility(View.GONE);
					groupText.setVisibility(View.GONE);
				} else {
					GetGroups task = new GetGroups();
					task.execute(parent.getItemAtPosition(position).toString());
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		group_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0){

				} else {
					String[] result = {type_spinner.getSelectedItem().toString(), faculty_spinner.getSelectedItem().toString(), parent.getItemAtPosition(position).toString()};
					mOnInputListener.getSelectionResult(result);
					dismiss();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		teacher_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0){

				} else {
					String[] result = {type_spinner.getSelectedItem().toString(), teacher_spinner.getSelectedItem().toString()};
					mOnInputListener.getSelectionResult(result);
					dismiss();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
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
			selectionProgressBar.setVisibility(RelativeLayout.VISIBLE);
		} else {
			getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
			selectionProgressBar.setVisibility(RelativeLayout.INVISIBLE);
		}
	}

	class GetFaculties extends AsyncTask<Void, Void, ArrayList<String>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressBarSetVisible(true);
		}

		@Override
		protected ArrayList<String> doInBackground(Void... voids) {
			ArrayList<String> faculties = new ArrayList<>();
			faculties.add("Не выбрано");
			DownloadSchedules parser = new DownloadSchedules(context);
			parser.getFaculties(faculties);
			return faculties;
		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {
			super.onPostExecute(null);
			faculty_adapter = new ArrayAdapter<String>(context, R.layout.spinner_item, result);
			faculty_adapter.setDropDownViewResource(R.layout.spinner_item);
			faculty_spinner.setAdapter(faculty_adapter);
			facultyText.setVisibility(View.VISIBLE);
			faculty_spinner.setVisibility(View.VISIBLE);
			groupText.setVisibility(View.GONE);
			group_spinner.setVisibility(View.GONE);
			teacherText.setVisibility(View.GONE);
			teacher_spinner.setVisibility(View.GONE);
			progressBarSetVisible(false);
		}
	}

	class GetGroups extends AsyncTask<String, Void, ArrayList<String>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressBarSetVisible(true);
		}

		@Override
		protected ArrayList<String> doInBackground(String... faculty) {
			ArrayList<String> groups = new ArrayList<>();
			groups.add("Не выбрано");
			DownloadSchedules parser = new DownloadSchedules(context);
			parser.getGroups(groups, faculty[0]);
			return groups;
		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {
			super.onPostExecute(null);
			group_adapter = new ArrayAdapter<String>(context, R.layout.spinner_item, result);
			group_adapter.setDropDownViewResource(R.layout.spinner_item);
			group_spinner.setAdapter(group_adapter);
			groupText.setVisibility(View.VISIBLE);
			group_spinner.setVisibility(View.VISIBLE);
			progressBarSetVisible(false);
		}
	}

	class GetTeachers extends AsyncTask<Void, Void, ArrayList<String>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressBarSetVisible(true);
		}

		@Override
		protected ArrayList<String> doInBackground(Void... voids) {
			ArrayList<String> teachers = new ArrayList<>();
			ArrayList<String> data = new ArrayList<>();
			data.add("Преподаватель");
			DownloadSchedules.getTeachersFromSite(teachers);
			Collections.sort(teachers);
			teachers.add(0, "Не выбрано");
			return teachers;
		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {
			super.onPostExecute(null);
			teacher_adapter = new ArrayAdapter<String>(context, R.layout.spinner_item, result);
			teacher_adapter.setDropDownViewResource(R.layout.spinner_item);
			teacher_spinner.setAdapter(teacher_adapter);
			teacherText.setVisibility(View.VISIBLE);
			teacher_spinner.setVisibility(View.VISIBLE);
			facultyText.setVisibility(View.GONE);
			faculty_spinner.setVisibility(View.GONE);
			groupText.setVisibility(View.GONE);
			group_spinner.setVisibility(View.GONE);
			progressBarSetVisible(false);
		}
	}
}
