package com.zgkx.change;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainUtil {
    public static final String KEY_AUTOCONN = "isAutoStart";
    /**
     * 获取今天年月日时分秒
     * @return 2017-08-14 11:53:52
     */
    public static String getTodayDateHms(String pattern) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.CHINA);
        return sdf.format(date);
    }

    public static String getFont(String str,boolean isRed){
        String colorStr=isRed?"#FF0000":"#04F40E";
        return new StringBuilder()
                .append("<font color=\""+colorStr+"\">")
                .append(str)
                .append("</font>").toString();
    }

}
