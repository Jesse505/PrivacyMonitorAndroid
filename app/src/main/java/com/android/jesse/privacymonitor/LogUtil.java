package com.android.jesse.privacymonitor;

import android.util.Log;

public class LogUtil {

    public static void trackViewOnClick(String monitorMethod, String privacyMethod) {
        Log.i("zyf", "在" + monitorMethod + "方法中调用了" + privacyMethod + "方法");
    }
}
