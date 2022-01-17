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
    private static final String TAG = "Utils";
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
     * 判断网络是否正常
     */
    public static boolean isNetNormal(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo.isAvailable() && networkInfo.isConnected();
    }

    /**
     * 获取SN号
     */
    public static String getSerialInfo(){
        return Build.VERSION.SDK_INT>=30?Build.getSerial():Build.SERIAL;
    }

}
