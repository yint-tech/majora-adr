package cn.iinti.majora.adr;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;

import cn.iinti.majora.adr.majora.MajoraClientService;
import cn.iinti.majora.adr.ui.WelcomeActivity;
import cn.iinti.majora.client.sdk.log.MajoraLogger;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

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
        if (launchIntent == null) {
            MajoraLogger.getLogger()
                    .warn("no launchIntent for package: " + getPackageName());
            return;
        }


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


    // 5. Drawable----> Bitmap
    public static Bitmap drawableToBitmap(Drawable drawable) {

        // 获取 drawable 长宽
        int width = drawable.getIntrinsicWidth();
        int heigh = drawable.getIntrinsicHeight();

        drawable.setBounds(0, 0, width, heigh);

        // 获取drawable的颜色格式
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        // 创建bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, heigh, config);
        // 创建bitmap画布
        Canvas canvas = new Canvas(bitmap);
        // 将drawable 内容画到画布中
        drawable.draw(canvas);
        return bitmap;
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
