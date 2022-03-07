package com.future.xlink.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.elvishew.xlog.XLog;
import com.future.xlink.bean.InitParams;

import java.util.UUID;

/**
 * @author chtj
 */
public class Utils {
    public static String getToken(InitParams params, String time) {
        String token = "Basic " + AESUtils.encrypt(params.key, params.sn + ":" + params.secret + ":" + time);
        return token;
    }

    /**
     * 创建消息数据唯一编号
     */
    public static String createIID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * [获取应用程序版本名称信息]
     *
     * @return 当前应用的版本名称
     */
    public static synchronized String getPackageName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.packageName;
        } catch (Throwable e) {
            XLog.e(e);
        }
        return null;
    }


    /**
     * 判断是否有网络连接
     */
    public static boolean isNetConnect(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isAvailable() && networkInfo.isConnected();
        } else {
            return false;
        }
    }
}
