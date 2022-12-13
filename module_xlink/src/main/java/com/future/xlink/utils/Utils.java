package com.future.xlink.utils;

import android.text.TextUtils;

import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chtj
 */
public class Utils {
    /**
     * 获取字符串中的正则表达式
     *
     * @param str 字符串
     * @return ip
     */
    public static String patternIp(String str) {
        String regular = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
        if (!TextUtils.isEmpty(str)) {
            Pattern pattern = Pattern.compile(regular);
            Matcher matcher = pattern.matcher(str);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return "";
    }

    /**
     * 判断string数组是否存在空的参数
     */
    public static boolean checkIsNull(String... params) {
        for (int i = 0; i < params.length; i++) {
            if (TextUtils.isEmpty(params[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * 比较获取最优的服务端链接
     */
    public static String compare(List<String> addrList) {
        List<String> bestAddrList=new ArrayList<>();
        for (int i = 0; i < addrList.size(); i++) {
            String pingResult= NetTools.ping(addrList.get(i),3,new StringBuffer());
            XLog.d( "compare: pingResult >> "+pingResult);
            bestAddrList.add(pingResult);
        }
        Collections.sort(bestAddrList, (s1, s2) -> {
            String[] o1 = s1.split("#");
            String[] o2 = s2.split("#");
            if (o1.length > o2.length) {
                return -1;
            } else if (o1.length < o2.length) {
                return 1;
            } else if ((o1.length == o2.length) && (o1.length == 1)) {
                return 1;
            } else {
                return Float.parseFloat(o1[1]) >= Float.parseFloat(o2[1]) ? 1 : -1;
            }

        });
        return bestAddrList.get(0).split("#")[0];
    }
}
