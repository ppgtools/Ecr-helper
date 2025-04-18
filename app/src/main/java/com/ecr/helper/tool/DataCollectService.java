package com.ecr.helper.tool;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.common.android_core.SupportLog;
import com.ecr.helper.app_constants_config_info.AppConfig_Constants;
import com.ecr.helper.app_constants_config_info.DataDetails;
import com.ecr.helper.base.MyEventBus;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import com.ecr.helper.tool.FileServer;

public class DataCollectService extends Service {
    private static final String TAG = DataCollectService.class.getSimpleName();
    private static final String CHANNEL_ID = "ECR_Helper_Tool";
    private FileServer fileServer;
    private Context context;
    private static final String ALGORITHM = "YourEncryptionKey";
    private static final int ITERATION_COUNT = 9999;

    public DataCollectService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        context = this; // Initialize the context
        MyEventBus.getEventBus().register(this);
        createNotificationChannel();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        MyEventBus.getEventBus().unregister(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check for files in the files folder and start the HTTP server with the last modified file if the server is not already running
        File filesDir = new File(AppConfig_Constants.SdardMp3RecLogs);
        File[] files = filesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
        File fileToServe;

        if (files != null && files.length > 0) {
            File lastModifiedFile = files[0];
            for (File file : files) {
                if (file.lastModified() > lastModifiedFile.lastModified()) {
                    lastModifiedFile = file;
                }
            }
            fileToServe = lastModifiedFile;
        } else {
            // Create an empty file named test.mp3 if no files are found
            fileToServe = new File(getFilesDir(), "test.mp3");
            try {
                if (!fileToServe.exists()) {
                    fileToServe.createNewFile();
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to create empty file", e);
            }
        }

        if (fileServer == null) {
            startHttpServerWithFile(fileToServe);
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call Recording Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void showNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("from_notification", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.call_recording_service))
                .setContentText(getString(R.string.service_not_fully_working))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }

    private byte[] encryptFile(String filePath, String password) throws Exception {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
        SecretKey secretKey = keyFactory.generateSecret(keySpec);

        byte[] salt = new byte[8];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, ITERATION_COUNT);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);

        byte[] fileContent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fileContent = Files.readAllBytes(Paths.get(filePath));
        }
        byte[] encryptedContent = cipher.doFinal(fileContent);

        byte[] encryptedFileWithSalt = new byte[salt.length + encryptedContent.length];
        System.arraycopy(salt, 0, encryptedFileWithSalt, 0, salt.length);
        System.arraycopy(encryptedContent, 0, encryptedFileWithSalt, salt.length, encryptedContent.length);

        return encryptedFileWithSalt;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onDataDetailsEvent(DataDetails event) {
        // receive the recording file path
// Stop the existing server if it's running
        if (fileServer != null) {
            Log.e("FileServer", "stop old file server: ");
            fileServer.stop();
        }

// Start a new HTTP server
        fileServer = new FileServer(10087, strAudioFullPath);
        try {
            Log.e("FileServer", "start new file server: ");
            fileServer.start();
        } catch (IOException ioe) {
            Log.e("FileServer", "Could not start server: " + ioe.getMessage());
            return;
        }

    }

    private void startHttpServerWithFile(File file) {
        String strAudioFullPath = file.getAbsolutePath();
        // Stop the existing server if it's running
        if (fileServer != null) {
            return;
        }

        // Start a new HTTP server
        fileServer = new FileServer(10087, strAudioFullPath);
        try {
            fileServer.start();
        } catch (IOException ioe) {
            Log.e("FileServer", "Could not start server: " + ioe.getMessage());
        }
    }
}