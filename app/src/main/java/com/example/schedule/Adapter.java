package com.example.schedule;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

class Adapter extends PagedListAdapter<Day, ViewHolder> {

    private final Context context;

    protected Adapter(DiffUtil.ItemCallback<Day> diffUtilCallback, Context context) {
        super(diffUtilCallback);
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Day day = getItem(position);
        if(day != null){
            holder.bind(day);
        }
    }


}