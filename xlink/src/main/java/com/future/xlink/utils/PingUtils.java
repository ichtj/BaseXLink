package com.future.xlink.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author chtj
 */
public class PingUtils {
    private static final String TAG = "PingUtils";

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
                Log.d(TAG, "ping ping fail:process is null.");
                append(stringBuffer, "ping fail:process is null.");
                return result;
            }
            successReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = successReader.readLine()) != null) {
                Log.d(TAG, "ping line-->" + line);
                if (line.contains("rtt min/avg/max/mdev = ")) {
                    //获取avg数据返回
                    result = host + "#" + parsedata(line);
                }
                append(stringBuffer, line);
            }
            int status = process.waitFor();
            if (status == 0) {
                Log.d(TAG, "ping exec cmd success:" + command);
                append(stringBuffer, "exec cmd success:" + command);
            } else {
                Log.d(TAG, "ping exec cmd fail.");
                append(stringBuffer, "exec cmd fail.");
            }
            Log.d(TAG, "ping exec finished.");
            append(stringBuffer, "exec finished.");
        } catch (InterruptedException e) {
            Log.e(TAG, "ping", e);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
            if (successReader != null) {
                try {
                    successReader.close();
                } catch (IOException e) {
                    Log.e(TAG, "ping", e);
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
            String ip = "114.114.114.114";
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 3 " + ip);// ping网址3次
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
            Log.d(TAG, "ping: ", e);
        }
        return false;
    }

    /**
     * 判断网络是否异常
     */
    public static boolean checkNetWork() {
        String[] PING_ADDR = new String[]{
                "114.114.114.114", "223.5.5.5", "180.76.76.76", "8.8.8.8"
        };
        for (int i = 0; i < PING_ADDR.length; i++) {
            boolean isNetOk = ping(PING_ADDR[i], 1, 2);
            if (!isNetOk) {
                //If it is abnormal when entering the program network at the beginning, then only prompt once
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    public static final boolean ping(String ip, int count, int time) {
        try {
            Process p = Runtime.getRuntime().exec("ping -c " + count + " -w " + time + " " + ip);
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
            Log.d(TAG, "ping: ", var15);
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
            String command = "ping -c " + 3 + " " + pingHosts[0];
            process = Runtime.getRuntime().exec(command);
            if (process == null) {
                Log.d(TAG, "ping ping fail:process is null.");
                return false;
            }
            int status = process.waitFor();
            if (status == 0) {
                Log.d(TAG, "ping exec cmd success:" + command);
                return true;
            }
        } catch (Throwable e) {
            Log.e(TAG, "ping: ",e );
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
