package com.example.schedule;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class DiffUtilCallback extends DiffUtil.ItemCallback<Day> {
    @Override
    public boolean areItemsTheSame(@NonNull Day oldDay,
                                   @NonNull Day newDay) {
        return oldDay.getDate().equals(newDay.getDate()) && oldDay.getGroup().equals(newDay.getGroup());
    }

    @Override
    public boolean areContentsTheSame(@NonNull Day oldDay,
                                      @NonNull Day newDay) {
        return oldDay.equals(newDay);
    }
}
