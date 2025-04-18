package com.ecr.helper.tool;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.ecr.helper.app_constants_config_info.AppConfig;
import com.ecr.helper.app_constants_config_info.AppConfig_Constants;
import com.ecr.helper.base.MyEventBus;

import com.ecr.helper.module_callrecorder.CallContactInfoEvent;
import com.ecr.helper.module_callrecorder.VoipNotificationEvent;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BackendService extends Service {


    @Inject
    CallContactInfoEvent callContactInfoEvent;

    @Inject
    VoipNotificationEvent voipNotificationEvent;


    @Inject
    AppConfig appConfig;



    @Override
    public void onCreate() {
        super.onCreate();

        // Refresh State
        IntentFilter updateFilter = new IntentFilter(AppConfig_Constants.strUpdaterStateAction);


        // init
        modifyState();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


        remove();
    }

    private void modifyState() {
        try {


            if (appConfig.getCallRecordState() == AppConfig.STATE_ENABLED) {
                if (callContactInfoEvent == null) {
                    callContactInfoEvent = new CallContactInfoEvent(appConfig);
                }
            } else {
                if (callContactInfoEvent != null) {
                    MyEventBus.getEventBus().unregister(callContactInfoEvent);
                    callContactInfoEvent = null;
                }
            }

            if (appConfig.getCallRecordState() == AppConfig.STATE_ENABLED) {
                if (voipNotificationEvent == null) {
                    voipNotificationEvent = new VoipNotificationEvent(appConfig);
                }
            } else {
                if (voipNotificationEvent != null) {
                    MyEventBus.getEventBus().unregister(voipNotificationEvent);
                    voipNotificationEvent = null;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void remove() {


        if (callContactInfoEvent != null) {
            callContactInfoEvent.unregister();
            callContactInfoEvent = null;
        }
        if (voipNotificationEvent != null) {
            voipNotificationEvent.unregister();
            voipNotificationEvent = null;
        }
    }

}