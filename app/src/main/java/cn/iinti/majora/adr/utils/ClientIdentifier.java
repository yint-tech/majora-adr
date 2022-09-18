package cn.iinti.majora.adr.utils;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import cn.iinti.majora.adr.TheApp;
import cn.iinti.majora.client.sdk.log.MajoraLogger;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;

public class ClientIdentifier {


    private static final String clientIdFileName = "majora_client_id.txt";
    private static final String UN_RESOLVE = "un_resolve_";

    private static String clientIdInMemory;

    public static String id() {
        if (clientIdInMemory != null) {
            return clientIdInMemory;
        }
        // from cache file
        File file = resolveIdCacheFile();
        if (file.exists()) {
            try {
                String s = CommonUtils.readFile(file);
                if (s != null && !s.isEmpty() && !s.startsWith(UN_RESOLVE)) {
                    clientIdInMemory = s;
                    return clientIdInMemory;
                }
            } catch (IOException e) {
                MajoraLogger.getLogger().error("can not read id file: " + file.getAbsolutePath(), e);
            }
        }

        clientIdInMemory = generateClientId() + "_" + UUID.randomUUID().toString();
        try {
            CommonUtils.writeFile(file, clientIdInMemory);
        } catch (IOException e) {
            MajoraLogger.getLogger().error("can not write id file: " + file.getAbsolutePath(), e);
        }
        return clientIdInMemory;
    }


    private static String generateClientId() {

        String s = generateClientIdForAndroid();
        if (s != null && !s.isEmpty()) {
            return s;
        }

        String mac = generateClientIdForNormalJVM();
        if (!TextUtils.isEmpty(mac)) {
            return mac;
        }
        return UN_RESOLVE + UUID.randomUUID().toString();
    }

    private static String generateClientIdForNormalJVM() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isVirtual()) {
                    continue;
                }
                if (networkInterface.isLoopback()) {
                    continue;
                }

                byte[] hardwareAddress = networkInterface.getHardwareAddress();
                if (hardwareAddress == null) {
                    continue;
                }
                return parseByte(hardwareAddress[0]) + ":" +
                        parseByte(hardwareAddress[1]) + ":" +
                        parseByte(hardwareAddress[2]) + ":" +
                        parseByte(hardwareAddress[3]) + ":" +
                        parseByte(hardwareAddress[4]) + ":" +
                        parseByte(hardwareAddress[5]);
            }
            return null;
        } catch (SocketException e) {
            return null;
        }
    }


    private static String parseByte(byte b) {
        int intValue;
        if (b >= 0) {
            intValue = b;
        } else {
            intValue = 256 + b;
        }
        return Integer.toHexString(intValue);
    }

    private static String generateClientIdForAndroid() {
        Application application = TheApp.getApplication();
        if (application.checkPermission(Manifest.permission.READ_PHONE_STATE, Process.myPid(), Process.myUid())
                == PackageManager.PERMISSION_GRANTED
        ) {
            TelephonyManager telephonyManager = (TelephonyManager) application.getSystemService(Context.TELEPHONY_SERVICE);
            String imei;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                imei = telephonyManager.getImei();
            } else {
                imei = telephonyManager.getDeviceId();
            }
            if (!TextUtils.isEmpty(imei)) {
                return imei;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String serial = Build.getSerial();
                if (!TextUtils.isEmpty(serial)) {
                    return serial;
                }
            }
        } else {
            MajoraLogger.getLogger().warn("need permission :" + Manifest.permission.READ_PHONE_STATE);
        }

        String serial = Build.SERIAL;
        if ("unknown".equalsIgnoreCase(serial)) {
            return "";
        }
        return serial;
    }

    private static File resolveIdCacheFile() {
        Application application = TheApp.getApplication();
        return new File(application.getFilesDir(), clientIdFileName);
    }


    public static boolean reset() {
        return resolveIdCacheFile().delete();
    }
}

