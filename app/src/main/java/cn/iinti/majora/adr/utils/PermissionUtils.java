package cn.iinti.majora.adr.utils;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.widget.Toast;

import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import cn.iinti.majora.adr.TheApp;
import cn.iinti.majora.client.sdk.log.MajoraLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PermissionUtils {

    public static boolean checkPermission() {
        return XXPermissions.isGranted(TheApp.getApplication(), minPermissions);
    }


    public static final String[] minPermissions = new String[]{
            Permission.READ_PHONE_STATE,
            Permission.SYSTEM_ALERT_WINDOW,
//            Permission.SYSTEM_OVERLAY_WINDOW,
//            Permission.FOREGROUND_SERVICE
    };

    public static void doGrantPermission(Activity context) {
        if (!checkFloatPermission(context)) {
            MajoraLogger.getLogger().info("请给软件设置悬浮窗权限，否则可能影响后台网络！");
            Toast.makeText(context, "请给软件设置悬浮窗权限，否则可能影响后台网络！", Toast.LENGTH_SHORT).show();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                context.startActivity(intent);
            }
        }

        if (!canBackgroundStart(context)) {
            Toast.makeText(context, "请给软件设置允许后台启动页面权限，否则可能影响后台网络！", Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean checkFloatPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                Class cls = Class.forName("android.content.Context");
                Field declaredField = cls.getDeclaredField("APP_OPS_SERVICE");
                declaredField.setAccessible(true);
                Object obj = declaredField.get(cls);
                if (!(obj instanceof String)) {
                    return false;
                }
                String str2 = (String) obj;
                obj = cls.getMethod("getSystemService", String.class).invoke(context, str2);
                cls = Class.forName("android.app.AppOpsManager");
                Field declaredField2 = cls.getDeclaredField("MODE_ALLOWED");
                declaredField2.setAccessible(true);
                Method checkOp = cls.getMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
                int result = (Integer) checkOp.invoke(obj, 24, Binder.getCallingUid(), context.getPackageName());
                return result == declaredField2.getInt(cls);
            } catch (Exception e) {
                return false;
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppOpsManager appOpsMgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                if (appOpsMgr == null)
                    return false;
                int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window", android.os.Process.myUid(), context
                        .getPackageName());
                return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED;
            } else {
                return Settings.canDrawOverlays(context);
            }
        }
    }

    private static boolean canBackgroundStart(Context context) {
        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        try {
            int op = 10021; // >= 23
            // ops.checkOpNoThrow(op, uid, packageName)
            Method method = ops.getClass().getMethod("checkOpNoThrow", int.class, int.class, String.class);
            Integer result = (Integer) method.invoke(ops, op, Process.myUid(), context.getPackageName());
            return result == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            MajoraLogger.getLogger().info("not support exec checkOpNoThrow", e);
            return true;
        }
    }
}
