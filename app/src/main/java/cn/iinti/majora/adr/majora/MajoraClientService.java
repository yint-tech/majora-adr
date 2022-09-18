package cn.iinti.majora.adr.majora;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import cn.iinti.majora.adr.TheApp;
import cn.iinti.majora.adr.utils.ClientIdentifier;
import cn.iinti.majora.adr.utils.CommonUtils;
import cn.iinti.majora.adr.utils.PermissionUtils;
import cn.iinti.majora.client.sdk.client.MajoraClient;
import cn.iinti.majora.client.sdk.cmd.handlers.RedialHandler;
import cn.iinti.majora.client.sdk.log.MajoraLogger;

import java.util.Map;

import eu.chainfire.libsuperuser.Shell;

public class MajoraClientService {
    private static boolean started = false;
    private static MajoraClient majoraClient;

    private static class RemoteRedialOperator implements RedialHandler.RedialOperator {

        @Override
        public String envOk() {
            if (!Shell.SU.available()) {
                // 没有root权限，那就无法进行飞行模式切换
                return "redial need rot permission";
            }
            return null;
        }

        @Override
        public void doRedial(Map<String, String> param) {
            String offlineWaitMilisStr = param.get("offlineWaitMilis");
            int offlineWaitMilis = 0;
            if (offlineWaitMilisStr != null) {
                try {
                    offlineWaitMilis = Integer.parseInt(offlineWaitMilisStr);
                } catch (NumberFormatException ignore) {
                }
            }
            reDial(offlineWaitMilis);
        }
    }

    // 重播任务定时器
    private static class RedialThread extends Thread {
        private static final RedialThread mInstance = new RedialThread();

        @Override
        public void run() {
            while (Thread.currentThread().isAlive()) {
                SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(TheApp.getApplication());
                boolean autoRedial = spf.getBoolean("auto_redial", true);
                int sleepMinutes = spf.getInt("auto_redial_duration", 5);
                if (sleepMinutes < 1) {
                    sleepMinutes = 1;
                }
                if (sleepMinutes > 600) {
                    sleepMinutes = 600;
                }
                if (!autoRedial) {
                    sleepMinutes = 5;
                }
                try {
                    Thread.sleep(sleepMinutes * 60 * 1000);
                } catch (InterruptedException e) {
                    //ignore
                    return;
                }
                if (autoRedial) {
                    reDial(2000);
                }
            }
        }
    }


    public static void startup() {
        if (started) {
            return;
        }

        if (!PermissionUtils.checkPermission()) {
            return;
        }

        startProxyService();
        RedialThread redialThread = RedialThread.mInstance;
        if (!redialThread.isAlive()) {
            redialThread.start();
        }
        started = true;
    }


    public static void reDial(int offlineWaitMilis) {
        if (!Shell.SU.available()) {
            // 没有root权限，那就无法进行飞行模式切换
            return;
        }
        if (offlineWaitMilis > 20_000) {
            offlineWaitMilis = 20_000;
        }
        if (majoraClient == null) {
            reDialImpl();
        } else {
            int finalOfflineWaitMilis = offlineWaitMilis;
            majoraClient.prepareReDial(() -> new Thread("redial-thread") {
                @Override
                public void run() {
                    if (finalOfflineWaitMilis > 0) {
                        try {
                            // 重播的时候，先摘除流量，等待一秒的时间
                            Thread.sleep(finalOfflineWaitMilis);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    reDialImpl();
                }
            }.start());
        }
    }

    private static void reDialImpl() {
        try {
            Shell.SU.run("settings put global airplane_mode_on 1 && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true");

            Thread.sleep(5000);
            Shell.SU.run("settings put global airplane_mode_on 0 &&  am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false");
        } catch (Exception e) {
            MajoraLogger.getLogger().info("reDial error", e);
        }
    }

    private static void startProxyService() {
        SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(TheApp.getApplication());
        String serverHost = spf.getString("server_host", "majora.iinti.cn");
        int serverPort = CommonUtils.toInt(spf.getString("server_port", "5879"), 5879);
        majoraClient = new MajoraClient(serverHost, serverPort, ClientIdentifier.id());
        majoraClient.setDeviceAccount(spf.getString("account_identifier", ""));

        RedialHandler.setRedialOperator(new RemoteRedialOperator());

        UILoggerHelper.setupLogger();
    }
}
