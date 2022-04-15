package com.future.xlink.utils;


import android.util.Log;

import com.elvishew.xlog.XLog;

import java.io.File;

/**
 * @author chtj
 */
public class GlobalConfig {
    public static final String ROOT_PATH = "/sdcard/xlink/";
    public static final String MY_PROPERTIES = "my.properties";
    public static final String AGENT_SERVER_LIST = "api/iot/reg/device/servers"; //代理服务端地址
    public static final String AGENT_REGISTER = "/api/iot/reg/device/register"; //注册服务器
    public static final String UPLOAD_LOGURL = "api/iot/reg/device/uploadLogUrl"; //获取上传日志信息
    public static final String UPLOAD_FILE = "api/iot/reg/device/file/upload"; //上传文件
    public static final String PRODUCT_DEVICE_INFO = "api/iot/reg/device/info"; //获取产品下的该设备信息
    public static final String PRODUCT_UNIQUE = "api/iot/reg/device/unique"; //设备sn唯一性验证
    public static final int OVER_TIME = 10 * 60 * 1000; //消息处理超时，默认10分钟

    /**
     * 远程控制指令
     */
    public static final int TYPE_REMOTE_RX           = 0x200;//消息接收
    public static final int TYPE_REMOTE_TX           = 0x201;//消息上报
    public static final int TYPE_REMOTE_TX_SERVICE   = 0x202;//属性服务上报
    public static final int TYPE_REMOTE_TX_EVENT     = 0x203;//事件上报

    public static final int TYPE_MODE_INIT_RX        = 0x204;//获取初始化参数
    public static final int TYPE_MODE_TO_CONNECT     = 0x205;//创建连接
    public static final int TYPE_MODE_CONNECT_RESULT = 0x206;//连接结果 变动的状态
    public static final int TYPE_MODE_CONNECT_LOST   = 0x207;//连接丢失


    public static void delProperties(String path){
        try {
            boolean isDel=new File(path).delete();
            String newPath=path.replace("/xlink","");
            boolean isDel2=new File(newPath).delete();
            Log.d("GlobalConfig", "delProperties: isDel="+isDel+",isDel2="+isDel2);
        }catch (Exception e){
            Log.e("GlobalConfig", "delProperties: ",e );
        }
        //删除一些旧的连接参数
    }
}
