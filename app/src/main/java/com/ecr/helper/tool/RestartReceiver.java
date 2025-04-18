package com.ecr.helper.tool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ecr.helper.module_callrecorder.CallingRecordService;

public class RestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            context.startService(new Intent(context, BackendService.class));
            context.startService(new Intent(context, DataCollectService.class));
        }
    }
}