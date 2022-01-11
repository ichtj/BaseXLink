package com.future.xlink.utils;

import com.elvishew.xlog.printer.file.naming.FileNameGenerator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DataFormatFileInfo implements FileNameGenerator {
    ThreadLocal<SimpleDateFormat> mLocalDateFormat = new ThreadLocal<SimpleDateFormat>() {
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        }
    };
    @Override
    public boolean isFileNameChangeable() {
        return false;
    }

    @Override
    public String generateFileName(int i, long l) {
        SimpleDateFormat sdf = this.mLocalDateFormat.get();
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(l))+"-xlink.log";
    }
}
