package cn.iinti.majora.adr;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import java.io.File;

import eu.chainfire.libsuperuser.Shell;

public class TheApp extends Application {
    private File logFile;
    private static TheApp theApp;

    public static TheApp getApplication() {
        return theApp;
    }


    public File getLogFile() {
        return logFile;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        theApp = this;
        new Thread("makeSu") {
            @Override
            public void run() {
                Shell.SU.available();
            }
        }.start();

        createLogFile();

        setNotifyChannel(this);
        KeepAliveService.startService(this);

    }

    private void createLogFile() {
        File file = new File(getFilesDir(), "log");
        if (!file.exists()) {
            file.mkdirs();
        }
        logFile = new File(file, "majora.log");
    }


    private static void setNotifyChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        @SuppressLint("WrongConstant") NotificationChannel notificationChannel = new NotificationChannel(
                BuildConfig.APPLICATION_ID,
                "channel", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.YELLOW);
        notificationChannel.setShowBadge(true);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        manager.createNotificationChannel(notificationChannel);
    }


}
