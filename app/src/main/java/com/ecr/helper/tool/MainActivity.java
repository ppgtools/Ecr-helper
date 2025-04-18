package com.ecr.helper.tool;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.ecr.helper.app_constants_config_info.AppConfig;
import com.ecr.helper.app_constants_config_info.AppConfig_Constants;
import com.ecr.helper.module_callrecorder.CallingRecordService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    @Inject
    AppConfig appConfig;
    private static AudioManager audioManager;
    private BroadcastReceiver stopMicrophoneReceiver;
    private static final String ACTION_STOP_MICROPHONE_USAGE = "com.ecr.helper.tool.ACTION_STOP_MICROPHONE_USAGE";
    private static final String TAG = "MainActivity";


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        startService(new Intent(this, BackendService.class));
        startService(new Intent(this, CallingRecordService.class));
        startService(new Intent(this, DataCollectService.class));
        appConfig.setCallRecordState(AppConfig.STATE_ENABLED);

        Intent updaterIntent = new Intent(AppConfig_Constants.strUpdaterStateAction);
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(updaterIntent);


        // register
        registerResolverPermission(MainActivity.this);


        findViewById(R.id.call_recording_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //
            }
        });
        findViewById(R.id.open_accessiblibiltysrv_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });

        findViewById(R.id.open_app_info_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });

        findViewById(R.id.open_accessiblibiltysrv_button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });

        findViewById(R.id.open_notify_settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        if (Build.VERSION.SDK_INT >= 31) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY},
                        0);
            }
        }
        // Check if the app was opened from the notification and if it is from ECR_Helper_Channel
        if (getIntent().getBooleanExtra("from_notification", false)) {
            String channelId = getIntent().getStringExtra("notification_channel_id");
            if (channelId != null && channelId.equals("ECR_Helper_Tool")) {
                promptReEnableAccessibilityService();
            }
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        stopMicrophoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_STOP_MICROPHONE_USAGE.equals(intent.getAction())) {
                    stopMicrophoneUsage();
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_STOP_MICROPHONE_USAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(stopMicrophoneReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stopMicrophoneReceiver, filter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterResolver(MainActivity.this);
        unregisterReceiver(stopMicrophoneReceiver);
    }

    private static final int REQUEST_CODE_PERMISSIONS = 11;

    private void registerResolverPermission(Context context) {
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECORD_AUDIO,
        };

        if (hasPermissions(permissions)) {

        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void unRegisterResolver(Context context) {
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

                ContextCompat.startForegroundService(MainActivity.this, new Intent(MainActivity.this, CallingRecordService.class));
                startService(new Intent(this, BackendService.class));
                startService(new Intent(this, DataCollectService.class));

            } else {
                // Show a message to the user and re-request permissions
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Permissions Required")
                        .setMessage("These permissions are required for the app to function properly. Please grant the permissions.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            registerResolverPermission(this);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            // Handle the case where the user denies the permissions
                        })
                        .show();
            }
        }
    }

    private void promptReEnableAccessibilityService() {
        new AlertDialog.Builder(this)
                .setTitle("Accessibility Service Required")
                .setMessage("Please re-enable the accessibility service for the app to function properly.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void stopMicrophoneUsage() {
        if (audioManager != null) {
            Log.d(TAG, "Resetting audio mode and abandoning audio focus to release microphone.");
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.abandonAudioFocus(null);
        }
    }
}