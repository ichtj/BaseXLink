package com.future.xlink.xlog;

import android.content.Context;

import com.library.logtools.BufferType;
import com.library.logtools.FaceLogTools;
import com.library.logtools.Level;

public class XLogTools {
    private static final String TAG = "XLogTools";
    /**
     * Log level for XLog.v.
     */
    public static final int VERBOSE = 2;

    /**
     * Log level for XLog.d.
     */
    public static final int DEBUG = 3;

    /**
     * Log level for XLog.i.
     */
    public static final int INFO = 4;

    /**
     * Log level for XLog.w.
     */
    public static final int WARN = 5;

    /**
     * Log level for XLog.e.
     */
    public static final int ERROR = 6;

    /**
     * 初始化时创建配置文件
     */
    public static void initXLog(Context context,String xlogPath) {
        FaceLogTools.initialize(context, true);
        FaceLogTools.putLogDirectory(xlogPath);
        d(TAG, "xlogpath>>"+xlogPath);
    }

    public static void v(String tag,String msg){
        record(tag,msg, VERBOSE);
    }

    public static void d(String tag,String msg){
        record(tag,msg, DEBUG);
    }

    public static void i(String tag,String msg){
        record(tag,msg, INFO);
    }

    public static void e(String tag,String msg) {
        record(tag,msg, ERROR);
    }

    public static void record(String tag,String msg,int level) {
        switch (level) {
            case VERBOSE:
                FaceLogTools.write(Level.V, BufferType.MAIN,android.os.Process.myPid(),tag,msg,true, true);
                break;
            case DEBUG:
                FaceLogTools.write(Level.D, BufferType.MAIN,android.os.Process.myPid(),tag,msg,true, true);
                break;
            case INFO:
                FaceLogTools.write(Level.I, BufferType.MAIN,android.os.Process.myPid(),tag,msg,true, true);
                break;
            case ERROR:
                FaceLogTools.write(Level.E, BufferType.MAIN,android.os.Process.myPid(),tag,msg,true, true);
                break;
        }
    }
}
