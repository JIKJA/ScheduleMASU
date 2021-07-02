package com.example.schedule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.navigation.NavigationView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, FullScreenDialog.OnInputListener {

    final String LOG_TAG = "Log";

    private DrawerLayout drawer;
    MenuItem reports_btn;
    RelativeLayout mainProgressBar;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    ArrayList<String> selectedData;

    DatasourceFactory sourceFactory;
    PagedList.Config config;
    LiveData<PagedList<Day>> pagedList;
    LinearLayoutManager linearLayoutManager;
    Adapter adapter;
    RecyclerView recyclerView;

    BroadcastReceiver br;
    public final static String INVALIDATE_ACTION = "com.example.schedule.invalidateBroadcast";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawer = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // shared prefs listener
        sharedPref = getSharedPreferences("com.example.schedule", MODE_PRIVATE);
        editor = sharedPref.edit();
        editor.commit();
        // check if first app run
        if (sharedPref.getBoolean("firstrun", true)) {
            editor.putBoolean("firstrun", false).commit();
            // select initial data
            if (isServiceRunning(FoneService.class)) {
                stopService(new Intent(this, FoneService.class));
            }
            DialogFragment dialog = FullScreenDialog.newInstance(getApplicationContext());
            dialog.show(getFragmentManager(), "selection_dialog");
        }

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.change_selection: {
                        if (isServiceRunning(FoneService.class)) {
                            stopService(new Intent(getApplicationContext(), FoneService.class));
                        }
                        DialogFragment dialog = FullScreenDialog.newInstance(getApplicationContext());
                        dialog.show(getFragmentManager(), "selection_dialog");

                        break;
                    }
                    case R.id.set_date: {
                        Calendar dateAndTime = Calendar.getInstance();
                        new DatePickerDialog(MainActivity.this, R.style.Theme_Schedule_DatePickerDialog, dateListener,
                                dateAndTime.get(Calendar.YEAR),
                                dateAndTime.get(Calendar.MONTH),
                                dateAndTime.get(Calendar.DAY_OF_MONTH))
                                .show();
                        break;
                    }
                    case R.id.open_reports: {
                        Intent intent = new Intent(getApplicationContext(), Reports.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.service_switch: {
                        return false;
                    }
                }
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        //switch setup
        SwitchCompat service_switch = navigationView.getMenu().findItem(R.id.service_switch).getActionView().findViewById(R.id.service_switch_switch);
        reports_btn = navigationView.getMenu().findItem(R.id.open_reports);
        if (sharedPref.getBoolean("continue_service_work", true)) {
            service_switch.setChecked(true);
        } else {
            service_switch.setChecked(false);
        }
        service_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    editor.putBoolean("continue_service_work", true).commit();
                } else {
                    editor.putBoolean("continue_service_work", false).commit();
                }
            }
        });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // load saved settings
        selectedData = new ArrayList<>();
        selectedData.add(sharedPref.getString("type", ""));
        if (selectedData.get(0).equals("Студент")) {
            selectedData.add(sharedPref.getString("faculty", ""));
            selectedData.add(sharedPref.getString("group", ""));
            getSupportActionBar().setTitle(sharedPref.getString("group", "Расписание"));
            reports_btn.setVisible(false);
        } else if (selectedData.get(0).equals("Преподаватель")) {
            selectedData.add(sharedPref.getString("teacher", ""));
            getSupportActionBar().setTitle(sharedPref.getString("teacher", "Расписание"));
            reports_btn.setVisible(true);
        }

        // create recyclerview
        sourceFactory = new DatasourceFactory(this, selectedData);
        // PagedList
        config = new PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPrefetchDistance(10)
                .setPageSize(30)
                .build();
        pagedList = new LivePagedListBuilder<>(sourceFactory, config)
                .setFetchExecutor(Executors.newSingleThreadExecutor())
                .build();
        // Adapter
        adapter = new Adapter(new DiffUtilCallback(), this);
        pagedList.observe(this, new Observer<PagedList<Day>>() {
            @Override
            public void onChanged(@Nullable PagedList<Day> days) {
                adapter.submitList(days);
            }
        });
        // recyclerView
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

        // get data from service
        br = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                TextView date = (TextView) linearLayoutManager.findViewByPosition(linearLayoutManager.findFirstVisibleItemPosition()).findViewById(R.id.date);
                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
                Calendar cal = Calendar.getInstance();
                try {
                    cal.setTime(format.parse(date.getText().toString().substring(0, 10)));
                } catch(ParseException e) {}
                sourceFactory.setData(selectedData);
                pagedList = new LivePagedListBuilder<>(sourceFactory, config)
                        .setFetchExecutor(Executors.newSingleThreadExecutor())
                        .setInitialLoadKey(cal)
                        .build();
                adapter.submitList(null);
                pagedList.observe(MainActivity.this, new Observer<PagedList<Day>>() {
                    @Override
                    public void onChanged(@Nullable PagedList<Day> days) {
                        adapter.submitList(days);
                    }
                });
                //pagedList.getValue().getDataSource().invalidate();
            }
        };
        IntentFilter intFilt = new IntentFilter(INVALIDATE_ACTION);
        registerReceiver(br, intFilt);

        // progress bar
        mainProgressBar = findViewById(R.id.main_progressbar);
        // register listener for progress bar
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        // progressbar open on start activity
        if (sharedPref.getBoolean("serviceBusy", false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            mainProgressBar.setVisibility(RelativeLayout.VISIBLE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            mainProgressBar.setVisibility(RelativeLayout.INVISIBLE);
        }

        // strat service
        if (!isServiceRunning(FoneService.class)) {
            startService(new Intent(this, FoneService.class));
        }
    }

    // datePicker listener
    DatePickerDialog.OnDateSetListener dateListener = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Calendar newDate = Calendar.getInstance();
            newDate.set(Calendar.YEAR, year);
            newDate.set(Calendar.MONTH, monthOfYear);
            newDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            pagedList = new LivePagedListBuilder<>(sourceFactory, config)
                    .setFetchExecutor(Executors.newSingleThreadExecutor())
                    .setInitialLoadKey(newDate)
                    .build();
            adapter.submitList(null);
            pagedList.observe(MainActivity.this, new Observer<PagedList<Day>>() {
                @Override
                public void onChanged(@Nullable PagedList<Day> days) {
                    adapter.submitList(days);
                }
            });
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("serviceBusy")){
            boolean activityStatus = sharedPreferences.getBoolean("serviceBusy", false);
            if (activityStatus) {
                if(!this.isFinishing())
                {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    mainProgressBar.setVisibility(RelativeLayout.VISIBLE);
                }
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                mainProgressBar.setVisibility(RelativeLayout.INVISIBLE);
            }
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void getSelectionResult(String[] result) {
        selectedData.clear();
        if (result.length == 3){
            selectedData.add(result[0]);
            editor.putString("type", selectedData.get(0)).commit();
            selectedData.add(result[1]);
            editor.putString("faculty", selectedData.get(1)).commit();
            selectedData.add(result[2]);
            editor.putString("group", selectedData.get(2)).commit();
            getSupportActionBar().setTitle(sharedPref.getString("group", "Расписание"));
            reports_btn.setVisible(false);
        } else if (result.length == 2){
            selectedData.add(result[0]);
            editor.putString("type", selectedData.get(0)).commit();
            selectedData.add(result[1]);
            editor.putString("teacher", selectedData.get(1)).commit();
            getSupportActionBar().setTitle(sharedPref.getString("teacher", "Расписание"));
            reports_btn.setVisible(true);
        } else {
            Log.e("Error", "Bad response from ");
        }
        Intent intent = new Intent(this, FoneService.class);
        startService(intent);
    }


    @Override
    protected void onStart() {
        super.onStart();
        editor.putInt("activityRunnning", 1).commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        editor.putInt("activityRunnning", 0).commit();
    }

    @Override
    protected void onDestroy() {
        if (!sharedPref.getBoolean("continue_service_work", true)){
            if (isServiceRunning(FoneService.class)) {
                stopService(new Intent(getApplicationContext(), FoneService.class));
            }
        }
        super.onDestroy();
    }
}