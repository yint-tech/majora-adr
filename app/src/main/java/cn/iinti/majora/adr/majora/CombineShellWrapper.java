package cn.iinti.majora.adr.majora;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.Collections;
import java.util.List;

import cn.iinti.majora.adr.BuildConfig;
import cn.iinti.majora.adr.IShellService;
import cn.iinti.majora.client.sdk.log.MajoraLogger;
import eu.chainfire.libsuperuser.Shell;
import rikka.shizuku.Shizuku;

/**
 * call airplane switch by su/shizhuku
 * <br>
 * 飞行模式的shell包装，将会支持su权限或者adb权限<br>
 * 如果你的手机已经root了，那么将会首选使用系统su权限进行重播<br>
 * 如果你的手机没有root，
 * <ul>
 *      <li>打开手机的开发者模式</li>，
 *      <li>安装shizuku app: https://github.com/RikkaApps/Shizuku</li>
 *      <li>根据shizuku的引导，配置shizuku</li>
 *      <li>在shizuku给majora app进行授权</li>
 * </ul>
 */
public class CombineShellWrapper {

    private static boolean shizukuAvailable = false;

    private static boolean hasShizukuPermission = false;

    private static IShellService shellService = null;

    private static final Shizuku.OnBinderReceivedListener BINDER_RECEIVED_LISTENER = () -> {
        if (Shizuku.getVersion() < 10) {
            // not support
            return;
        }
        shizukuAvailable = true;
        if (!Shizuku.isPreV11() && Shizuku.checkSelfPermission() != PERMISSION_GRANTED) {
            Shizuku.requestPermission(111);
        } else {
            hasShizukuPermission = true;
            bindIShellService();
        }
    };
    private static final Shizuku.OnBinderDeadListener BINDER_DEAD_LISTENER = () -> shizukuAvailable = false;
    private static final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = (int requestCode, int grantResult) -> {
        hasShizukuPermission = grantResult == PERMISSION_GRANTED;
        bindIShellService();
    };


    // 需要是public，否则shizhuku在反射的时候无法创建远端service
    public static final class ShizukuShellService extends IShellService.Stub {

        @Override
        public List<String> run(String cmd) {
            // run command on shell(the adb) process
            return Shell.SH.run(cmd);
        }
    }


    private static final Shizuku.UserServiceArgs userServiceArgs =
            new Shizuku.UserServiceArgs(new ComponentName(BuildConfig.APPLICATION_ID, ShizukuShellService.class.getName()))
                    .daemon(false)
                    .processNameSuffix("service")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE);

    private static void bindIShellService() {
        if (!hasShizukuPermission) {
            return;
        }
        Shizuku.bindUserService(userServiceArgs, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                shellService = IShellService.Stub.asInterface(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                shellService = null;
            }
        });
    }


    public static void initShizhuku() {
        Shizuku.addBinderReceivedListenerSticky(BINDER_RECEIVED_LISTENER);
        Shizuku.addBinderDeadListener(BINDER_DEAD_LISTENER);
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        new Thread("makeSu") {
            @Override
            public void run() {
                Shell.SU.available();
            }
        }.start();
    }


    public static boolean available() {
        return (shizukuAvailable && hasShizukuPermission && shellService != null) || Shell.SU.available();
    }

    public static List<String> run(String cmd) {
        if (Shell.SU.available()) {
            MajoraLogger.getLogger().info("run cmd with su: " + cmd);
            return Shell.SU.run(cmd);
        }
        if ((shizukuAvailable && hasShizukuPermission && shellService != null)) {
            // use shizuku
            IShellService localShellService = shellService;
            try {
                MajoraLogger.getLogger().info("run cmd with shizhuku: " + cmd);
                return localShellService.run(cmd);
            } catch (RemoteException e) {
                MajoraLogger.getLogger().warn("execute remote cmd error", e);
                return Collections.emptyList();
            }
        }
        MajoraLogger.getLogger().warn("call redial ,but no su or shizuku available");
        return Collections.emptyList();
    }
}
