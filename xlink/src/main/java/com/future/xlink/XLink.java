package com.future.xlink;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.Printer;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.Protocal;
import com.future.xlink.bean.common.ConnectType;
import com.future.xlink.bean.common.RespType;
import com.future.xlink.bean.mqtt.RespStatus;
import com.future.xlink.listener.MessageListener;
import com.future.xlink.mqtt.MqttManager;
import com.future.xlink.mqtt.RxMqttService;
import com.future.xlink.utils.Carrier;
import com.future.xlink.utils.DataFormatFileInfo;
import com.future.xlink.utils.DefaultFlattenerInfo;
import com.future.xlink.utils.GlobalConfig;
import com.future.xlink.utils.GsonUtils;
/*import com.future.xlink.utils.PropertiesUtil;*/
import com.future.xlink.utils.Utils;
import com.future.xlink.utils.XBus;

import java.io.File;

import io.reactivex.annotations.NonNull;

/**
 * @author chtj
 */
public class XLink {
    //代表1天1L * 24L * 60L * 60L * 1000L
    //代表30天1L * 24L * 60L * 60L * 1000L
    private static final long MAX_TIME=1L * 24L * 60L * 60L * 1000L;
    /**
     * 单例
     */
    private static XLink mInstance = null;
    /**
     * 上下文参数
     */
    private Context context;
    /**
     * 消息回调接口
     */
    private MessageListener listener;

    private XLink() {

    }


    public MessageListener getListener() {
        return listener;
    }

    public static XLink getInstance() {
        if (null == mInstance) {
            mInstance = new XLink();
        }
        return mInstance;
    }

    /**
     * xlink初始化
     *
     * @param context  初始化句柄
     * @param params   初始化参数类
     * @param listener 初始化回调函数
     */
    public void init(@NonNull Context context, @NonNull InitParams params, @NonNull MessageListener listener) {
        String pkgName = Utils.getPackageName(context);
        GlobalConfig.PROPERT_URL = GlobalConfig.SYS_ROOT_PATH + pkgName + "/" + params.getSn() + "/";
        createAndInitXLog(pkgName, params.getSn());//创建info,error日志的存储路径和.log文件my.properties文件
        this.context = context;
        this.listener = listener;
        Intent intent = new Intent(context, RxMqttService.class);
        intent.putExtra(RxMqttService.INIT_PARAM, params);
        context.startService(intent);
    }



    /**
     * 初始化时创建配置文件
     */
    private void createAndInitXLog(String pkgName,String sn) {
        creatFile();
        LogConfiguration config = new LogConfiguration.Builder()
                .logLevel(LogLevel.ALL)   // 指定日志级别，低于该级别的日志将不会被打印，默认为 LogLevel.ALL
                .tag("XLink")             // 指定 TAG，默认为 "X-LOG"
                //.enableThreadInfo()       // 允许打印线程信息，默认禁止
                //.enableStackTrace(2)      // 允许打印深度为 2 的调用栈信息，默认禁止
                //.enableBorder()         // 允许打印日志边框，默认禁止
                .build();

        String xlogPath=GlobalConfig.SYS_ROOT_PATH+pkgName+ "/"+ sn+"/"+"xlink-log/";
        Printer androidPrinter = new AndroidPrinter(true);// 通过 android.util.Log 打印日志的打印器
        //Printer consolePrinter = new ConsolePrinter();            // 通过 System.out 打印日志到控制台的打印器
        Printer filePrinter = new FilePrinter                       // 打印日志到文件的打印器
                .Builder(xlogPath)                                  // 指定保存日志文件的路径
                .fileNameGenerator(new DataFormatFileInfo())        // 指定日志文件名生成器，默认为 ChangelessFileNameGenerator("log")
                .backupStrategy(new NeverBackupStrategy())          // 指定日志文件备份策略，默认为 FileSizeBackupStrategy(1024 * 1024)
                .cleanStrategy(new FileLastModifiedCleanStrategy(MAX_TIME))     // 指定日志文件清除策略，默认为 NeverCleanStrategy()
                .flattener(new DefaultFlattenerInfo())
                .build();
        XLog.init(                                                  // 初始化 XLog
                config,                                             // 指定日志配置，如果不指定，会默认使用 new LogConfiguration.Builder().build()
                androidPrinter,                                     // 添加任意多的打印器。如果没有添加任何打印器，会默认使用 AndroidPrinter(Android)/ConsolePrinter(java)
                //consolePrinter,
                filePrinter);
    }
    private void creatFile(){
        //创建err文件夹
        File errLogFile = new File(GlobalConfig.PROPERT_URL);
        if (!errLogFile.exists()) {
            errLogFile.mkdirs();
        }
        //创建err文件夹
        File myPropertiesFile = new File(GlobalConfig.PROPERT_URL+GlobalConfig.MY_PROPERTIES);
        if (!myPropertiesFile.exists()) {
            myPropertiesFile.mkdirs();
        }
    }

    /**
     * 创建连接函数
     */
    public void connect() {
        XBus.post(new Carrier(Carrier.TYPE_MODE_TO_CONNECT));
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        XBus.post(new Carrier(Carrier.TYPE_MODE_CONNECT_RESULT, ConnectType.CONNECT_DISCONNECT));
    }


    /**
     * 注销函数，注销后，重连mqtt需要重新调用init()函数
     */
    public void unInit() {
        context.stopService(new Intent(context, RxMqttService.class));
        GlobalConfig.delProperties();
        this.listener = null;
        this.context = null;
    }

    /**
     * 上报服务属性函数
     *
     * @param protocal 属性消息封装
     */
    public void upService(Protocal protocal) {
        publish(Carrier.TYPE_REMOTE_TX_SERVICE, protocal);
    }

    /**
     * 上报事件函数
     *
     * @param protocal 事件消息封装
     */
    public void upEvent(Protocal protocal) {
        publish(Carrier.TYPE_REMOTE_TX_EVENT, protocal);
    }

    /**
     * 代理服务端请求响应事件
     *
     */
    public void upResponse(Protocal protocal) {
        publish(Carrier.TYPE_REMOTE_TX, protocal);
    }

    private void publish(int type, Protocal protocal) {
        if (MqttManager.getInstance().isConnect()) {
            //状态为连接 才能添加消息到集合
            XBus.post(new Carrier(type, protocal));
        } else {
            if (this.listener != null) {
                //交互已经出现问题,回调该消息异常
                protocal.rx = GsonUtils.toJsonWtihNullField(new RespStatus(RespType.RESP_CONNECT_LOST.getTye(), RespType.RESP_CONNECT_LOST.getValue()));
                this.listener.messageArrived(protocal);
            }
        }
    }

    /**
     * 获取Mqtt连接状态
     * @return true连接 false断开
     */
    public boolean isConnected(){
        return MqttManager.getInstance().isConnect();
    }
}
