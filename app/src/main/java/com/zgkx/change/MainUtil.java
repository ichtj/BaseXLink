package com.zgkx.change;

import android.util.Log;

import com.future.xlink.bean.PutType;
import com.future.xlink.request.DataTransfer;
import com.future.xlink.request.XLink;
import com.future.xlink.utils.GsonTools;
import com.zgkx.change.testbean.DoorRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainUtil {
    private final static String TAG=MainUtil.class.getSimpleName();
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

    public static void pushTestEvent(){
        String jsonStr="{\n" +
                "\t\"recordId\": \"3FD95F0BD5501464604001D75F0B\",\n" +
                "\t\"person\": {\n" +
                "\t\t\"personId\": \"e74a43f5-e536-423e-b056-4747c0723cb8\",\n" +
                "\t\t\"personNo\": \"130279\",\n" +
                "\t\t\"personName\": \"窝窝头呢\",\n" +
                "\t\t\"personGender\": 2,\n" +
                "\t\t\"deptId\": \"5a9dc528-e7ad-4952-9f21-a885b6564a1c\",\n" +
                "\t\t\"deptName\": \"汉唐大厦;|汉唐大厦;\",\n" +
                "\t\t\"remark\": null,\n" +
                "\t\t\"personPhoto\": \"http://192.168.10" +
                ".100:9012/down/pic/25200803/door/2a5233e3/2a5233e3_3032471336.jpg\",\n" +
                "\t\t\"certificateType\": \"其他\",\n" +
                "\t\t\"identityNo\": \"\",\n" +
                "\t\t\"mobile\": \"18665899427\",\n" +
                "\t\t\"tel1\": null,\n" +
                "\t\t\"tel2\": null,\n" +
                "\t\t\"email\": \"\",\n" +
                "\t\t\"roomNo\": \"\",\n" +
                "\t\t\"address\": \"\",\n" +
                "\t\t\"tenementType\": 2,\n" +
                "\t\t\"vehicleList\": null\n" +
                "\t},\n" +
                "\t\"remark\": \"开门方式不匹配\",\n" +
                "\t\"device\": {\n" +
                "\t\t\"deviceGuid\": \"2c6f8bc5-73e3-4ddc-bdaa-4d6d22703167\",\n" +
                "\t\t\"deviceId\": \"190830336\",\n" +
                "\t\t\"deviceName\": \"中出口\",\n" +
                "\t\t\"deviceIp\": \"192.168.10.30\",\n" +
                "\t\t\"deviceGateway\": \"192.168.10.1\",\n" +
                "\t\t\"deviceNetmask\": \"255.255.255.0\",\n" +
                "\t\t\"deviceType\": 0,\n" +
                "\t\t\"deviceIoType\": 0,\n" +
                "\t\t\"parentId\": \"3945515520\",\n" +
                "\t\t\"parentName\": null,\n" +
                "\t\t\"remark\": \"通过搜索设备添加\"\n" +
                "\t},\n" +
                "\t\"cardNo\": \"2A5233E3\",\n" +
                "\t\"cardType\": 169,\n" +
                "\t\"eventType\": 16,\n" +
                "\t\"recordType\": 2,\n" +
                "\t\"inOutType\": 0,\n" +
                "\t\"crossTime\": \"2023-03-17 11:36:53\",\n" +
                "\t\"offlineFlag\": 0,\n" +
                "\t\"interviewee\": null,\n" +
                "\t\"pictureFile\": \"http://192.168.10" +
                ".100:9012//down/pic/20230317/door/190830336/00_47_2a5233e3_641450d5.jpg\",\n" +
                "\t\"errorNo\": 4120,\n" +
                "\t\"errorName\": \"开门方式不匹配\",\n" +
                "\t\"TemperatureDevID\": 0,\n" +
                "\t\"TemperatureDevName\": null,\n" +
                "\t\"Temperature\": 0.0,\n" +
                "\t\"reTrySend\": null\n" +
                "}";
        try {
            DoorRecord doorRecord= GsonTools.fromJson(jsonStr,DoorRecord.class);
            HashMap map=new HashMap();
            map.put("eventType",doorRecord.getEventType());
            map.put("recordType",doorRecord.getRecordType());
            map.put("pictureFile",doorRecord.getPictureFile());
            map.put("crossTime",doorRecord.getCrossTime());
            map.put("cardNo",doorRecord.getCardNo());
            map.put("cardType",doorRecord.getCardType());
            map.put("device",doorRecord.getDevice());
            map.put("remark",doorRecord.getRemark());
            map.put("person",doorRecord.getPerson());
            map.put("recordId",doorRecord.getRecordId());
            map.put("interviewee",GsonTools.toJsonWtihNullField(doorRecord.getInterviewee()));
            Log.d(TAG,"doorRecord info >>> "+doorRecord);
            XLink.putCmd(PutType.EVENT, DataTransfer.createIID(),"door_record", map);
        }catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
    }

    public static void pushTestEvent2(){
        String jsonStr="{\n" +
                "\t\"recordId\": \"7EDCD80AF2F31964000001DCD80A\",\n" +
                "\t\"person\": {\n" +
                "\t\t\"personId\": \"eb4854ec-cd3c-46b7-94e2-c73a8d27ffec\",\n" +
                "\t\t\"personNo\": \"10057008\",\n" +
                "\t\t\"personName\": \"陶陶\",\n" +
                "\t\t\"personGender\": 2,\n" +
                "\t\t\"deptId\": \"\",\n" +
                "\t\t\"deptName\": \"\",\n" +
                "\t\t\"remark\": \"第三方SDK下发邀访记录时发行凭证\",\n" +
                "\t\t\"personPhoto\": \"\",\n" +
                "\t\t\"certificateType\": \"其他\",\n" +
                "\t\t\"identityNo\": null,\n" +
                "\t\t\"mobile\": \"15688552551\",\n" +
                "\t\t\"tel1\": null,\n" +
                "\t\t\"tel2\": null,\n" +
                "\t\t\"email\": null,\n" +
                "\t\t\"roomNo\": null,\n" +
                "\t\t\"address\": null,\n" +
                "\t\t\"tenementType\": 7,\n" +
                "\t\t\"vehicleList\": null\n" +
                "\t},\n" +
                "\t\"remark\": \"二维码用户无权限\",\n" +
                "\t\"device\": {\n" +
                "\t\t\"deviceGuid\": \"b195f443-1132-4f2f-8815-13c478079c33\",\n" +
                "\t\t\"deviceId\": \"181984256\",\n" +
                "\t\t\"deviceName\": \"012\",\n" +
                "\t\t\"deviceIp\": \"192.168.10.26\",\n" +
                "\t\t\"deviceGateway\": \"192.168.10.1\",\n" +
                "\t\t\"deviceNetmask\": \"255.255.255.0\",\n" +
                "\t\t\"deviceType\": 0,\n" +
                "\t\t\"deviceIoType\": 0,\n" +
                "\t\t\"parentId\": \"3945515520\",\n" +
                "\t\t\"parentName\": null,\n" +
                "\t\t\"remark\": \"通过搜索设备添加\"\n" +
                "\t},\n" +
                "\t\"cardNo\": \"00006621BDCD\",\n" +
                "\t\"cardType\": 56,\n" +
                "\t\"eventType\": 16,\n" +
                "\t\"recordType\": 2,\n" +
                "\t\"inOutType\": 0,\n" +
                "\t\"crossTime\": \"2023-03-21 18:14:10\",\n" +
                "\t\"offlineFlag\": 0,\n" +
                "\t\"interviewee\": {\n" +
                "\t\t\"personId\": \"d78efd7b-dbb2-4f59-9e65-0007961f6a03\",\n" +
                "\t\t\"personNo\": \"89008566\",\n" +
                "\t\t\"personName\": \"黎清晨\",\n" +
                "\t\t\"personGender\": 2,\n" +
                "\t\t\"deptId\": \"5a9dc528-e7ad-4952-9f21-a885b6564a1c\",\n" +
                "\t\t\"deptName\": \"汉唐大厦;|汉唐大厦;\",\n" +
                "\t\t\"remark\": null,\n" +
                "\t\t\"personPhoto\": \"down/pic/25220323/door/a40bafe3/a40bafe3_3079028974" +
                ".jpg\",\n" +
                "\t\t\"certificateType\": \"IDENTITY\",\n" +
                "\t\t\"identityNo\": \"\",\n" +
                "\t\t\"mobile\": \"13713907538\",\n" +
                "\t\t\"tel1\": null,\n" +
                "\t\t\"tel2\": null,\n" +
                "\t\t\"email\": \"\",\n" +
                "\t\t\"roomNo\": null,\n" +
                "\t\t\"address\": \"\",\n" +
                "\t\t\"tenementType\": 2,\n" +
                "\t\t\"vehicleList\": null\n" +
                "\t},\n" +
                "\t\"pictureFile\": null,\n" +
                "\t\"errorNo\": 4136,\n" +
                "\t\"errorName\": \"二维码用户无权限\",\n" +
                "\t\"TemperatureDevID\": 0,\n" +
                "\t\"TemperatureDevName\": null,\n" +
                "\t\"Temperature\": 0.0,\n" +
                "\t\"reTrySend\": null\n" +
                "}";
        try {
            DoorRecord doorRecord= GsonTools.fromJson(jsonStr,DoorRecord.class);
            HashMap map=new HashMap();
            map.put("eventType",doorRecord.getEventType());
            map.put("recordType",doorRecord.getRecordType());
            map.put("pictureFile",doorRecord.getPictureFile());
            map.put("crossTime",doorRecord.getCrossTime());
            map.put("cardNo",doorRecord.getCardNo());
            map.put("cardType",doorRecord.getCardType());
            map.put("device",GsonTools.toJsonWtihNullField(doorRecord.getDevice()));
            map.put("remark",doorRecord.getRemark());
            map.put("person",GsonTools.toJsonWtihNullField(doorRecord.getPerson()));
            map.put("recordId",doorRecord.getRecordId());
            map.put("interviewee",GsonTools.toJsonWtihNullField(doorRecord.getInterviewee()));
            Log.d(TAG,"doorRecord info >>> "+doorRecord);
            XLink.putCmd(PutType.EVENT, DataTransfer.createIID(),"door_record", map);
        }catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
    }
}
