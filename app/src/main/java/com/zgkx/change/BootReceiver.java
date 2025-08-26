package com.zgkx.change;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"onReceive() >> "+Intent.ACTION_BOOT_COMPLETED);
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            intent= new Intent(context, MainAty.class);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
