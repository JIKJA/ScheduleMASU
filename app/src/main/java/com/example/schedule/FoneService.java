package com.example.schedule;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.core.app.NotificationCompat;

public class FoneService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{

    Thread thr;
    DownloadSchedules parser;
    ArrayList<String> data;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    String CHANNEL_ID = "com.example.schedule";
    Integer NOTIF_ID = 101;
    NotificationManager notificationManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i("FS", "FoneService - start");
        parser = new DownloadSchedules(this);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = getNotification("Ожидание обновления");

        startForeground(NOTIF_ID, notification);

        // load saved data and start main loop
        data = new ArrayList<>();
        sharedPref = getSharedPreferences("com.example.schedule", MODE_PRIVATE);
        editor = sharedPref.edit();
        editor.commit();
        data.add(sharedPref.getString("type", ""));
        if(data.get(0).equals("Студент")){
            data.add(sharedPref.getString("faculty", ""));
            data.add(sharedPref.getString("group", ""));
            startLoop();
        } else if(data.get(0).equals("Преподаватель")){
            data.add(sharedPref.getString("teacher", ""));
            startLoop();
        }
        // register listener to get info about mainactivity lifecycle events
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        return START_STICKY;
    }

    private void startLoop() {

        thr = new Thread(new Runnable() {
            public void run() {
                Notification notification;
                while (true) {
                    // add variable to shared pref
                    notification = getNotification("Обработка расписания");
                    notificationManager.notify(NOTIF_ID, notification);
                    editor.putBoolean("serviceBusy", true).commit();
                    boolean invalidateFlag = parser.proceedSchedule(data);
                    if (invalidateFlag) {
                        if (sharedPref.getInt("activityRunnning", -1) == 0){
                            notification = getNotification("Расписание обновлено");
                            notificationManager.notify(NOTIF_ID, notification);
                        } else{
                            notification = getNotification("Ожидание обновления");
                            notificationManager.notify(NOTIF_ID, notification);
                        }
                    } else {
                        notification = getNotification("Ожидание обновления");
                        notificationManager.notify(NOTIF_ID, notification);
                    }
                    Intent intent = new Intent(MainActivity.INVALIDATE_ACTION);
                    sendBroadcast(intent);
                    editor.putBoolean("serviceBusy", false).commit();
                    try {
                        thr.sleep(3600000); // 1 hour delay
                    } catch (InterruptedException e) {}
                }
            }
        });

        thr.setDaemon(true);
        thr.start();

    }

    private Notification getNotification(String text){
        // The PendingIntent to launch our activity if the user selects
        // this notification
        String CHANNEL_ID = "com.example.schedule";

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Service", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Background service");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setBadgeIconType(R.drawable.ic_stat_name)
                .setContentIntent(pendingIntent)
                .setNotificationSilent()
                .build();

        return notification;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("activityRunnning")){
            int activityStatus = sharedPreferences.getInt("activityRunnning",-1);
            if (activityStatus == 1) {
                Notification notification = getNotification("Ожидание обновления");
                notificationManager.notify(NOTIF_ID, notification);
            } else if (activityStatus == 0){

            }
        }
    }

}
