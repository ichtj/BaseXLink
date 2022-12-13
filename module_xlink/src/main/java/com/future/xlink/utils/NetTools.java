package com.future.xlink.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.elvishew.xlog.XLog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author chtj
 */
public class NetTools {
    private static final String TAG = NetTools.class.getSimpleName();
    /**
     * 获取ping 的耗时
     */
    public static String ping(String host, int pingCount, StringBuffer stringBuffer) {
        String result = null;
        String line = null;
        Process process = null;
        BufferedReader successReader = null;
        try {
            String url = null;
            if (host.startsWith("http://")) {
                url = host.substring("http://".length());
            } else if (host.startsWith("https://")) {
                url = host.substring("https://".length());
            } else {
                url = host;
            }
            String[] pingHosts = url.split(":");
            String command = "ping -c " + pingCount + " " + pingHosts[0];
            process = Runtime.getRuntime().exec(command);
            if (process == null) {
                XLog.d("ping ping fail:process is null.");
                append(stringBuffer, "ping fail:process is null.");
                return result;
            }
            successReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = successReader.readLine()) != null) {
                XLog.d("ping line-->" + line);
                if (line.contains("rtt min/avg/max/mdev = ")) {
                    //获取avg数据返回
                    result = host + "#" + parsedata(line);
                }
                append(stringBuffer, line);
            }
            int status = process.waitFor();
            if (status == 0) {
                XLog.d("ping exec cmd success:" + command);
                append(stringBuffer, "exec cmd success:" + command);
            } else {
                XLog.d("ping exec cmd fail.");
                append(stringBuffer, "exec cmd fail.");
            }
            XLog.d("ping exec finished.");
            append(stringBuffer, "exec finished.");
        } catch (Throwable e) {
            XLog.e("ping", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
            if (successReader != null) {
                try {
                    successReader.close();
                } catch (Throwable e) {
                    XLog.e(e);
                }
            }
        }
        return result;
    }

    /**
     * 获取集合中随机个数不重复的项，返回下标数组
     *
     * @param list 数据列表
     * @param num  随机数量
     * @return 子集
     */
    static String[] getRandomPingList(String[] list, int num) {
        List flagExist = new ArrayList();
        List<Integer> indexList = new ArrayList<Integer>();
        if (num >= list.length) {
            return list;
        }
        // 创建随机数
        Random random = new Random();
        // 当set长度不足 指定数量时
        while (flagExist.size() < num) {
            // 获取源集合的长度的随机数
            Integer index = random.nextInt(list.length);
            // 获取随机数下标对应的元素
            Object obj = list[index];
            // 不包含该元素时
            if (!flagExist.contains(obj)) {
                // 添加到集合中
                flagExist.add(obj);
                // 记录下标
                indexList.add(index);
            }
        }
        String[] pingList = new String[indexList.size()];
        for (int i = 0; i < indexList.size(); i++) {
            pingList[i] = list[indexList.get(i)];
        }
        return pingList;
    }

    /**
     * 判断网络是否异常
     */
    public static boolean checkNet(String[] pingList, Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            String[] newList = getRandomPingList(pingList, pingList.length<3?pingList.length:3);
            XLog.d("newList >> " + Arrays.toString(newList));
            for (int i = 0; i < newList.length; i++) {
                String pingAddr = newList[i];
                boolean isNetOk = ping(pingAddr, 1, 1);
                XLog.d("newList [" + i + "]>> " + pingAddr + " >> " + isNetOk);
                if (isNetOk) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * 判断是否有外网连接（普通方法不能判断外网的网络是否连接，比如连接上局域网）
     * 不要在主线程使用，会阻塞线程
     */
    static boolean ping(String ip, int count, int w) {
        return ping(ip, count, w, 0);
    }


    /**
     * 判断是否有外网连接（普通方法不能判断外网的网络是否连接，比如连接上局域网）
     * 不要在主线程使用，会阻塞线程
     */
    static boolean ping(String ip, int count, int w, int W) {
        try {
            StringBuilder cbstr = new StringBuilder("ping");
            if (count != 0) {
                cbstr.append(" -c " + count);
            }
            if (w != 0) {
                cbstr.append(" -w " + w);
            }
            if (W != 0) {
                cbstr.append(" -W " + W);
            }
            cbstr.append(" " + ip);
            String cmd = cbstr.toString();
            Process p = Runtime.getRuntime().exec(cmd);// ping网址3次
            // 读取ping的内容，可以不加
            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuilder stringBuffer = new StringBuilder();
            String content = "";
            while ((content = in.readLine()) != null) {
                stringBuffer.append(content);
            }
            // ping的状态
            int status = p.waitFor();
            Log.d(TAG, "ping: cmd >> " + cmd + ", content >> " + stringBuffer.toString() + ", status >> " + status);
            if (status == 0) {
                return true;
            }
        } catch (Throwable e) {
            Log.e(TAG, "ping: ", e);
        }
        return false;
    }

    static void append(StringBuffer stringBuffer, String text) {
        if (stringBuffer != null) {
            stringBuffer.append(text + "/n");
        }
    }

    static String parsedata(String line) {
        try {
            String end = line.substring("rtt min/avg/max/mdev = ".length());
            return end.split("/")[1];
        } catch (Throwable e) {
            XLog.e(e);
            return null;
        }
    }
}
