package com.example.schedule;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;

import java.util.ArrayList;
import java.util.Calendar;

public class DatasourceFactory extends DataSource.Factory<Calendar, Day> {
	private ArrayList<String> data;
	Context context;

	DatasourceFactory(Context context, ArrayList<String> data) {
		this.data = data;
		this.context = context;
	}

	@NonNull
	@Override
	public DataSource<Calendar, Day> create() {
		return new ScheduleDataSource(context, data);
	}

	public void setData(ArrayList<String> data){
		this.data = data;
	}
}
