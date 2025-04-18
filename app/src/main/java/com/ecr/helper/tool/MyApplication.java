package com.ecr.helper.tool;

import android.app.Application;
import android.content.Intent;

import com.common.android_core.LogUtils;
import com.common.android_core.ServiceUtils;
import com.ecr.helper.app_constants_config_info.AppConfig;
import com.ecr.helper.app_constants_config_info.AppConfig_Constants;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MyApplication extends Application {

    @Inject
    AppConfig appConfig;

    @Override
    public void onCreate() {
        super.onCreate();

        LogUtils.setlogger(false, "ecr_logs");

        init();

    }

    private void init() {
        new AppConfig_Constants(this);

        appConfig.setContext(this);
        appConfig.setAppName(getString(R.string.app_name));
        appConfig.setNotificationContentTitle(getString(R.string.app_name));
        appConfig.setNotificationContentText("ECR Helper is running...");
        appConfig.setSmallIconID(R.mipmap.ic_launcher);
        appConfig.setNotificationJumpActivity(MainActivity.class);

        appConfig.setImgFileFormat("yyyyMMddHHmmss");

        appConfig.setPhoneCallDir(AppConfig_Constants.Mp3RecLogs);
        appConfig.setVoipCallDir(AppConfig_Constants.Mp3RecLogs);
    }

}
