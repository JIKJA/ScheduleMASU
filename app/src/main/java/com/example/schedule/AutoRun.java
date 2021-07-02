package com.example.schedule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoRun extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			Log.d("AutoRun", "broadcast received");
			context.startService(new Intent(context, FoneService.class));
		}
	}
}
