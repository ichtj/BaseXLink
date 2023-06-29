package com.future.xlink.utils;

import com.elvishew.xlog.XLog;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.other.Register;
import com.future.xlink.request.retrofit.IApis;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileTools {

    /**
     * 初始化log文件
     */
    public static void initLogFile(String configFolder) {
        File configFile = new File(configFolder);
        if (!configFile.exists()) {
            configFile.mkdirs();
        }
    }

    /**
     * 读取文件内容
     *
     * @param fileName 路径+文件名称
     * @return 读取到的内容
     */
    public static String readFileData(String fileName) {
        String result = "";
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                return "";
            }
            FileInputStream fis = new FileInputStream(file);
            //获取文件长度
            int lenght = fis.available();
            byte[] buffer = new byte[lenght];
            fis.read(buffer);
            fis.close();
            //将byte数组转换成指定格式的字符串
            result = new String(buffer, "UTF-8");
        } catch (Throwable e) {
            XLog.e("readFileData>Throwable >> ", e);
        }
        return result;
    }

    /**
     * 写入数据
     *
     * @param filename 路径+文件名称
     * @param content  写入的内容
     * @param isCover  是否覆盖文件的内容 true 覆盖原文件内容  | flase 追加内容在最后
     * @return 是否成功 true|false
     */
    public static boolean writeFileData(String filename, String content, boolean isCover) {
        FileOutputStream fos = null;
        try {
            File file = new File(filename);
            //如果文件不存在
            if (!file.exists()) {
                //重新创建文件
                file.createNewFile();
            }
            fos = new FileOutputStream(file, !isCover);
            byte[] bytes = content.getBytes();
            fos.write(bytes);//将byte数组写入文件
            fos.flush();
            return true;
        } catch (Exception e) {
            XLog.e("writeFileData>Exception >> " + e.getMessage());
        } finally {
            try {
                fos.close();//关闭文件输出流
            } catch (Throwable e) {
                XLog.e("writeFileData>Throwable >> " + e.getMessage());
            }
        }
        return false;
    }


    /**
     * 删除一些旧的配置参数
     */
    public static void delProperties(String configPath) {
        try {
            boolean isDel = new File(configPath).delete();
            XLog.d("delProperties: isDel=" + isDel);
        } catch (Exception e) {
            XLog.e("delProperties>Exception>> ", e);
        }
    }

    /**
     * 保存配置到文件到指定位置
     */
    public static boolean saveConfig(InitParams params, String configFolder, String jsonData) {
        try {
            Register regPonse = GsonTools.fromJson(jsonData, Register.class);
            params.mqttBroker = regPonse.mqttBroker;
            params.mqttSsid = regPonse.ssid;
            params.mqttUsername = regPonse.mqttUsername;
            params.mqttPassword = regPonse.mqttPassword;
            String configSave = JsonFormat.formatJson(GsonTools.toJsonWtihNullField(params));
            return FileTools.writeFileData(configFolder + IApis.MY_PROPERTIES, configSave, true);
        } catch (JSONException e) {
            XLog.e("saveConfig>JSONException >> " + e.getMessage());
            return false;
        }
    }

    /**
     * 读取文件中的每一行内容到集合中去
     *
     * @return 返回读取到的所有包名list集合
     */
    public static List<String> readLineToList(String filePath) {
        //将读出来的一行行数据使用Map存储
        List<String> bmdList = new ArrayList<>();
        try {
            File file = new File(filePath);
            if (file.isFile() && file.exists()) {  //文件存在的前提
                InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
                BufferedReader br = new BufferedReader(isr);
                String lineTxt;
                while ((lineTxt = br.readLine()) != null) {  //
                    if (!"".equals(lineTxt)) {
                        String reds = lineTxt.split("\\+")[0];  //java 正则表达式
                        bmdList.add(reds);//依次放到集合中去
                    }
                }
                isr.close();
                br.close();
            } else {
                System.out.println("can not find file");//找不到文件情况下
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bmdList;
    }
}
