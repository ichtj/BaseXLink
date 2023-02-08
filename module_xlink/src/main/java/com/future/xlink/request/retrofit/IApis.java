package com.future.xlink.request.retrofit;
/**
 * @author chtj
 */
public interface IApis {
    String ROOT = "/sdcard/xlink/";
    String MY_PROPERTIES = "my.properties";
    String CONFIG_PATH = "/data/misc/localconfig.json";
    String AGENT_SERVER_LIST = "api/iot/reg/device/servers"; //代理服务端地址
    String AGENT_REGISTER = "/api/iot/reg/device/register"; //注册服务器
    String UPLOAD_LOGURL = "api/iot/reg/device/uploadLogUrl"; //获取上传日志信息
    String UPLOAD_FILE = "api/iot/reg/device/file/upload"; //上传文件
    String PRODUCT_DEVICE_INFO = "api/iot/reg/device/info"; //获取产品下的该设备信息
    String PRODUCT_UNIQUE = "api/iot/reg/device/unique"; //设备sn唯一性验证
}
