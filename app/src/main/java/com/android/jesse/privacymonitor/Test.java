package com.android.jesse.privacymonitor;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class Test {
    public static String getMacAddressByInterface() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if ("wlan0".equalsIgnoreCase(nif.getName())) {
                    byte[] macBytes = nif.getHardwareAddress();
                    if (macBytes == null) {
                        return "";
                    }

                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(String.format("%02X:", b));
                    }

                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    return res1.toString();
                }
            }

        } catch (Exception e) {
            //ignore
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean isForegroundApp(Context context, String pkgName) {
        ActivityManager am = context.getSystemService(ActivityManager.class);
        Log.i("", "");
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        return !tasks.isEmpty() && pkgName.equals(tasks.get(0).topActivity.getPackageName());
    }
}
