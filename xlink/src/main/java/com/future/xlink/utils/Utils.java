package com.future.xlink.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.elvishew.xlog.XLog;
import com.future.xlink.bean.InitParams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;

/**
 * @author chtj
 */
public class Utils {
    public static String getToken(InitParams params, String time) {
        String token = "Basic " + AESUtils.encrypt(params.getKey(), params.getSn() + ":" + params.getSecret() + ":" + time);
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
     * 读取文件内容
     *
     * @param fileName 路径+文件名称
     * @return 读取到的内容
     */
    public static String readFileData(String fileName) {
        String result = "";
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                return "";
            }
            FileInputStream fis = new FileInputStream(file);
            //获取文件长度
            int lenght = fis.available();
            byte[] buffer = new byte[lenght];
            fis.read(buffer);
            if (fis != null) {
                fis.close();
            }
            //将byte数组转换成指定格式的字符串
            result = new String(buffer, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Utils", "readFileData: ",e );
        }
        return result;
    }

    /**
     * 写入数据
     *
     * @param filename 路径+文件名称
     * @param content  写入的内容
     * @param isCover  是否覆盖文件的内容 true 覆盖原文件内容  | flase 追加内容在最后
     * @return 是否成功 true|false
     */
    public static boolean writeFileData(String filename, String content, boolean isCover) {
        FileOutputStream fos = null;
        try {
            File file = new File(filename);
            //如果文件不存在
            if (!file.exists()) {
                //重新创建文件
                file.createNewFile();
            }
            fos = new FileOutputStream(file, !isCover);
            byte[] bytes = content.getBytes();
            fos.write(bytes);//将byte数组写入文件
            fos.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Utils", "writeFileData: " + e.getMessage());
        } finally {
            try {
                fos.close();//关闭文件输出流
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Utils", "errMeg:" + e.getMessage());
            }
        }
        return false;
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
