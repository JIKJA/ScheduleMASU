package com.example.schedule;

import android.content.Context;
import android.content.SyncContext;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ReportRecyclerAdapter extends RecyclerView.Adapter<ReportRecyclerAdapter.ViewHolder> {

	private ArrayList<Day> localDataSet;
	private Context context;

	/**
	 * Provide a reference to the type of views that you are using
	 * (custom ViewHolder).
	 */
	public static class ViewHolder extends RecyclerView.ViewHolder {
		TextView date;
		TextView[] lesson_name = new TextView[7];
		TextView[] lesson_type = new TextView[7];
		TextView[] start_time = new TextView[7];
		TextView[] finish_time = new TextView[7];
		TextView[] teacher = new TextView[7];
		TextView[] place = new TextView[7];
		LinearLayout[] linLayout = new LinearLayout[7];

		public ViewHolder(View view, Context context) {
			super(view);
			date = (TextView) view.findViewById(R.id.date);
			for(int i=0; i<7; i++) {
				int id = context.getResources().getIdentifier("lesson"+(i+1), "id", context.getPackageName());
				LinearLayout layout = (LinearLayout) view.findViewById(id);
				linLayout[i] = layout;
				lesson_name[i] = (TextView) layout.findViewById(R.id.lesson_name);
				lesson_type[i] = (TextView) layout.findViewById(R.id.lesson_type);
				start_time[i] = (TextView) layout.findViewById(R.id.start_time);
				finish_time[i] = (TextView) layout.findViewById(R.id.finish_time);
				teacher[i] = (TextView) layout.findViewById(R.id.teacher);
				place[i] = (TextView) layout.findViewById(R.id.place);
			}
		}

		public void bind(Day day){
			for(int i=0; i<7; i++) {
				linLayout[i].setVisibility(View.GONE);
				lesson_name[i].setText("");
				lesson_type[i].setText("");
				start_time[i].setText("");
				finish_time[i].setText("");
				teacher[i].setText("");
				place[i].setText("");

			}
			date.setText(day.getDate());
			for(int i=0; i<day.getLessonsNumber(); i++){
				linLayout[i].setVisibility(View.VISIBLE);
				Day.Lesson lesson = day.getLesson(i);
				lesson_name[i].setText(lesson.lesson_name);
				lesson_type[i].setText(lesson.lesson_type);
				start_time[i].setText(lesson.start_time);
				finish_time[i].setText(lesson.finish_time);
				teacher[i].setText(lesson.teacher);
				place[i].setText(lesson.place);
			}
		}
	}

	public ReportRecyclerAdapter(ArrayList<Day> dataSet, Context context) {
		localDataSet = dataSet;
		this.context = context;
	}

	// Create new views (invoked by the layout manager)
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
		View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false);
		return new ReportRecyclerAdapter.ViewHolder(view, context);
	}

	// Replace the contents of a view (invoked by the layout manager)
	@Override
	public void onBindViewHolder(ViewHolder viewHolder, final int position) {
		viewHolder.bind(localDataSet.get(position));
	}

	// Return the size of your dataset (invoked by the layout manager)
	@Override
	public int getItemCount() {
		return localDataSet.size();
	}
}
