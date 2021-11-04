package com.future.xlink.utils;


/**
 * @author chtj
 */
public class GlobalConfig {
    public static final String SYS_ROOT_PATH = "/sdcard/xlink/";
    public static String PROPERT_URL = "";
    public static final String MY_PROPERTIES = "my.properties"; //参数存储文件
    public static final String HTTP_SERVER = "http://iot.frozenmoment.cn:10130/"; //生产测试环境
    public static final String AGENT_SERVER_LIST = "api/iot/reg/device/servers"; //代理服务端地址
    public static final String AGENT_REGISTER = "/api/iot/reg/device/register"; //注册服务器
    public static final String PRODUCT_UNIQUE = "api/iot/reg/device/unique"; //设备sn唯一性验证
    public static final String UPLOAD_LOGURL = "api/iot/reg/device/uploadLogUrl"; //获取上传日志信息
    public static final String UPLOAD_FILE = "api/iot/reg/device/file/upload"; //上传文件
    public static final int OVER_TIME = 10 * 60 * 1000; //消息处理超时，默认10分钟
}
