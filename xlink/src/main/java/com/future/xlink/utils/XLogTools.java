package com.future.xlink.utils;

import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.Printer;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;

import java.util.HashMap;
import java.util.Map;

public class XLogTools {
    private static Map<String,Logger> loggerMap=new HashMap<>();
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
    public static void initResetXLog(String xlogPath) {
        LogConfiguration config = new LogConfiguration.Builder()
                .logLevel(LogLevel.ALL)   // 指定日志级别，低于该级别的日志将不会被打印，默认为 LogLevel.ALL
                .tag("XLink")             // 指定 TAG，默认为 "X-LOG"
                //.enableThreadInfo()       // 允许打印线程信息，默认禁止
                //.enableStackTrace(2)      // 允许打印深度为 2 的调用栈信息，默认禁止
                //.enableBorder()         // 允许打印日志边框，默认禁止
                .build();
        Printer androidPrinter = new AndroidPrinter(true);// 通过 android.util.Log 打印日志的打印器
        //Printer consolePrinter = new ConsolePrinter();            // 通过 System.out 打印日志到控制台的打印器
        //代表1天1L * 24L * 60L * 60L * 1000L
        //代表30天1L * 24L * 60L * 60L * 1000L
        Printer filePrinter = new FilePrinter                       // 打印日志到文件的打印器
                .Builder(xlogPath)                                  // 指定保存日志文件的路径
                .fileNameGenerator(new DataFormatFileInfo())        // 指定日志文件名生成器，默认为 ChangelessFileNameGenerator("log")
                .backupStrategy(new NeverBackupStrategy())          // 指定日志文件备份策略，默认为 FileSizeBackupStrategy(1024 * 1024)
                .cleanStrategy(new FileLastModifiedCleanStrategy(1L * 24L * 60L * 60L * 1000L))     // 指定日志文件清除策略，默认为 NeverCleanStrategy()
                .flattener(new DefaultFlattenerInfo())
                .build();
        XLog.init(                                                  // 初始化 XLog
                config,                                             // 指定日志配置，如果不指定，会默认使用 new LogConfiguration.Builder().build()
                androidPrinter,                                     // 添加任意多的打印器。如果没有添加任何打印器，会默认使用 AndroidPrinter(Android)/ConsolePrinter(java)
                //consolePrinter,
                filePrinter);
        d("XLink>>>initXLog");
    }

    /**
     * debug log
     */
    public static void d(Object msg) {
        record(msg, DEBUG);
    }

    /**
     * error log
     */
    public static void e(Object msg) {
        record(msg, ERROR);
    }

    /**
     * 记录日志级别
     *
     * @param msg   内容
     * @param level 等级
     */
    public static void record(Object msg, int level) {
        switch (level) {
            case VERBOSE:
                XLog.v(msg);
                break;
            case DEBUG:
                XLog.d(msg);
                break;
            case INFO:
                XLog.i(msg);
                break;
            case WARN:
                XLog.w(msg);
                break;
            case ERROR:
                XLog.e(msg);
                break;
        }
    }

    public static void cd(String tag,Object msg){
        custom(tag,msg,DEBUG);
    }

    public static void ce(String tag,Object msg){
        custom(tag,msg,ERROR);
    }
    /**
     * 自定义日志
     * @param tag 标签
     * @param msg 内容
     * @param level 等级
     */
    public static void custom(String tag,Object msg,int level){
        Logger logger =null;
        if(loggerMap.containsKey(tag)){
            logger=loggerMap.get(tag);
        }else{
            if(XLog.assertInitialization()){
                logger= XLog.tag(tag).build();
                loggerMap.put(tag,logger);
            }else{
                return;
            }
        }
        switch (level) {
            case VERBOSE:
                logger.v(msg);
                break;
            case DEBUG:
                logger.d(msg);
                break;
            case INFO:
                logger.i(msg);
                break;
            case WARN:
                XLog.w(msg);
                break;
            case ERROR:
                logger.e(msg);
                break;
        }
    }
}
