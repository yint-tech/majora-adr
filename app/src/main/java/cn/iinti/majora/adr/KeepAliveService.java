package cn.iinti.majora.adr;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;

import cn.iinti.majora.adr.majora.MajoraClientService;
import cn.iinti.majora.adr.ui.WelcomeActivity;

public class KeepAliveService extends Service {
    private static boolean start = false;

    public static void startService(Context context) {
        if (start) {
            return;
        }
        Intent intent = new Intent(context, KeepAliveService.class);
        context.startService(intent);
    }


    private void onServiceStartupInternal() {
        Intent launchIntent = new Intent(this, WelcomeActivity.class);
        String channelId = BuildConfig.APPLICATION_ID;
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, launchIntent, FLAG_UPDATE_CURRENT))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("majora")
                .setContentText("majora")
                .setWhen(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(channelId);
        }

        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        startForeground(channelId.hashCode(), notification);
        MajoraClientService.startup();
        start = true;
    }


    @Override
    public IBinder onBind(Intent intent) {
        onServiceStartupInternal();
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onServiceStartupInternal();
        return START_STICKY;
    }
}
