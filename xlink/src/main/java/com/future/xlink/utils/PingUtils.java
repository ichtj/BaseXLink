package com.future.xlink.utils;

import com.elvishew.xlog.XLog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author chtj
 */
public class PingUtils {
    public static final String[] PING_ADDR = new String[]{
            /*"114.114.114.114",*/ "223.5.5.5", "223.6.6.6", "180.76.76.76",/* "8.8.8.8",*/
            /*"114.114.115.115",*/ "119.29.29.29", "210.2.4.8", /*"9.9.9.9", */"199.91.73.222",
            "101.226.4.6","1.2.4.8","47.106.129.104"
    };
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
            XLog.e("ping",e);
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
     * 判断是否有外网连接（普通方法不能判断外网的网络是否连接，比如连接上局域网）
     * 不要在主线程使用，会阻塞线程
     */
    public static final boolean ping() {
        try {
            // ping 的地址，可以换成任何一种可靠的外网
            String ip = "47.106.129.104";
            Process p = Runtime.getRuntime().exec("ping -c 2 -W 1 " + ip);// ping网址1次
            // 读取ping的内容，可以不加
            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuffer stringBuffer = new StringBuffer();
            String content = "";
            while ((content = in.readLine()) != null) {
                stringBuffer.append(content);
            }
            // ping的状态
            int status = p.waitFor();
            if (status == 0) {
                return true;
            }
        } catch (Throwable e) {
            XLog.e(e);
        }
        return false;
    }
    /**
     * 两重循环去重
     * 随机指定范围内N个不重复的数
     * 最简单最基本的方法
     * @param min 指定范围最小值
     * @param max 指定范围最大值
     * @param randomSize 随机数个数
     */
    public static int[] randomCommon(int min, int max, int randomSize) {
        if (randomSize > (max - min + 1) || max < min) {
            return null;
        }
        int[] result = new int[randomSize];
        int count = 0;
        while (count < randomSize) {
            int num = (int) (Math.random() * (max - min)) + min;
            boolean flag = true;
            for (int j = 0; j < randomSize; j++) {
                if (num == result[j]) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                result[count] = num;
                count++;
            }
        }
        return result;
    }
    /**
     * 判断网络是否异常
     */
    public static boolean checkNetWork() {
        int [] randomArray=randomCommon(0,PING_ADDR.length-1,3);
        for (int i = 0; i < randomArray.length; i++) {
            String pingAddr=PING_ADDR[randomArray[i]];
            boolean isNetOk = ping(pingAddr, 2, 1);
            if (!isNetOk) {
                //If it is abnormal when entering the program network at the beginning, then only prompt once
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    public static final boolean ping(String ip, int count, int second) {
        try {
            //-w 等待回复的时间，单位是毫秒。这个选项只在没有接到任何的回复的情况下有效，只要接到了一个回复，就将等待时间设置为两倍的RTT。如果没有设置，则等待时间设置为一个最大值
            //-W 等待回复的时间，单位是毫秒
            Process p = Runtime.getRuntime().exec("ping -c " + count + " -W " + second + " " + ip);
            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuffer stringBuffer = new StringBuffer();
            String content = "";
            while ((content = in.readLine()) != null) {
                stringBuffer.append(content);
            }
            int status = p.waitFor();
            if (status == 0) {
                return true;
            }
        } catch (Throwable var15) {
            XLog.e(var15);
        }
        return false;
    }


    public static boolean ping(String host) {
        Process process = null;
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
            String command = "ping -c " + 2 + "-W 1 " + pingHosts[0];
            process = Runtime.getRuntime().exec(command);
            if (process == null) {
                XLog.d("ping ping fail:process is null.");
                return false;
            }
            int status = process.waitFor();
            if (status == 0) {
                XLog.d("ping exec cmd success:" + command);
                return true;
            }
        } catch (Throwable e) {
            XLog.e(e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return false;
    }

    private static void append(StringBuffer stringBuffer, String text) {
        if (stringBuffer != null) {
            stringBuffer.append(text + "/n");
        }
    }

    private static String parsedata(String line) {
        try {
            String end = line.substring("rtt min/avg/max/mdev = ".length());
            return end.split("/")[1];
        } catch (Throwable e) {
            XLog.e(e);
            return null;
        }
    }
}
