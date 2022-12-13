package com.future.xlink.xlog;

import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.flattener.Flattener2;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DefaultFlattener implements  Flattener2 {

    @Override
    public CharSequence flatten(long timeMillis, int logLevel, String tag, String message) {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sd = sdf.format(new Date(Long.parseLong(String.valueOf(timeMillis))));
        return sd + '|' + LogLevel.getShortLevelName(logLevel) + '|' + tag + '|' + message;
    }
}
