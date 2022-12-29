package com.zgkx.change;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.elvishew.xlog.XLog;
import com.future.xlink.bean.base.BaseData;
import com.future.xlink.bean.PutType;
import com.future.xlink.request.DataTransfer;
import com.future.xlink.request.XLinkHttp;
import com.future.xlink.request.XLink;
import com.future.xlink.bean.InitParams;
import com.future.xlink.callback.IMqttCallback;
import com.future.xlink.callback.IHttpRequest;
import com.future.xlink.request.retrofit.IApis;
import com.future.xlink.utils.JsonFormat;
import com.future.xlink.utils.TheadTools;
import com.future.xlink.xlog.XLogTools;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainAty extends Activity implements IMqttCallback, View.OnClickListener {
    private static final String TAG = MainAty.class.getSimpleName() + "F";
    private Button btnSeHearbeat;
    private Button btnGetAgent;
    private Button btnGetDevice;
    private Button btnClear;
    private Button btnAddDev;
    private Button btnConn;
    private Button btnDisConn;
    private TextView tvResult;
    private TextView tvConnStatus;
    private TextView tvSn;
    private static boolean isHeartbeat = false;
    //private String clientId = "FSMMMNNNFF001";
    private String clientId = "FSMMMNNNFF002";
    private TimerThread timerThread;

    /**
     * 获取初始化参数
     *
     * @return 初始化对象
     */
    public InitParams getInitParams() {
        InitParams iParam = new InitParams();
        iParam.autoReConnect = true;
        iParam.clientId = clientId;
        iParam.appKey = "61SLS7nwJxllOodv";
        iParam.appSecret = "FeJsQqfc1xB7B8oSlqbd81a1lUsyaDGC";
        iParam.pdid = "22122a20-5ea8-40ef-b7d2-329ee4207474";
        iParam.httpUrl = "http://iot.frozenmoment.cn";
        iParam.httpPort = "10130";
        iParam.uniqueKey = "fswl1web1aes2020";
        iParam.uniqueScret = "fswl_device_auth_secret_v2020";
        return iParam;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvConnStatus = findViewById(R.id.tvConnStatus);
        tvSn = findViewById(R.id.tvSn);
        btnSeHearbeat = findViewById(R.id.btnSeHearbeat);
        btnGetAgent = findViewById(R.id.btnGetAgent);
        btnGetDevice = findViewById(R.id.btnGetDevice);
        btnClear = findViewById(R.id.btnClear);
        btnAddDev = findViewById(R.id.btnAddDev);
        btnDisConn = findViewById(R.id.btnDisConn);
        tvResult = findViewById(R.id.tvResult);
        tvResult.setMovementMethod(ScrollingMovementMethod.getInstance());
        btnConn = findViewById(R.id.btnConn);
        btnClear.setOnClickListener(this);
        btnAddDev.setOnClickListener(this);
        btnConn.setOnClickListener(this);
        btnDisConn.setOnClickListener(this);
        btnGetDevice.setOnClickListener(this);
        btnGetAgent.setOnClickListener(this);
        btnSeHearbeat.setOnClickListener(this);
        tvSn.setText("当前操作的SN：" + clientId);

        String configFolder = IApis.ROOT + getPackageName() + "/" + clientId + "/xlink-log/";
        XLogTools.initXLog(configFolder);
        timerThread = new TimerThread(handler);
        timerThread.start();
    }

    /**
     * 连接状态变化
     *
     * @param connected   true false
     * @param description connected 为false时描述错误信息
     */
    @Override
    public void connState(boolean connected, String description) {
        sendToMessage(MainUtil.getFont("connState()", !connected) + " >> connected=" + connected + ",description=" + description + "<br />");
        if (connected) {
            XLink.subscribe("dev/" + clientId + "/#", 2);
            Map<String,Object> maps=new HashMap<>();
            maps.put("address","飞思未来深圳科技有限公司");
            maps.put("longitude","0");
            maps.put("latitude","0");
            XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "online", maps);
        }
        handler.sendMessage(handler.obtainMessage(0x101, connected));
    }

    /**
     * 平台指令到达
     *
     * @param baseData 消息内容
     */
    @Override
    public void msgArrives(BaseData baseData) {
        sendToMessage(MainUtil.getFont("msgArrives()", false) + " >> " + baseData.toString() + "<br />");
        if (baseData.operation.equals("remote_cmd")) {
            Map<String, Object> cmdList = new HashMap<>();
            cmdList.put("data", "这是一个测试data");
            cmdList.put("result", "这是一个测试result");
            cmdList.put("description", "这是一个测试description");
            XLink.putCmd(PutType.METHOD, baseData.iid, baseData.operation, cmdList);
        } else if (baseData.operation.equals("16")) {
            if (baseData.iPutType == PutType.GETPERTIES) {
                Map<String, Object> maps = new HashMap<>();
                maps.put(baseData.maps.get("prid").toString(),true);
                XLink.putCmd(PutType.GETPERTIES, baseData.iid, baseData.operation, maps);
            } else if (baseData.iPutType == PutType.SETPERTIES) {
                XLink.putCmd(PutType.SETPERTIES, baseData.iid, baseData.operation, baseData.maps);
            }
        }
    }

    /**
     * 推送完成
     *
     * @param baseData 完成的数据
     */
    @Override
    public void pushed(BaseData baseData) {
        sendToMessage(MainUtil.getFont("pushed()", false) + " >> " + baseData.toString() + "<br />");
    }

    /**
     * 平台回复消息
     *
     * @param act
     * @param iid
     */
    @Override
    public void iotReplyed(String act, String iid) {
        sendToMessage(MainUtil.getFont("iotReplyed()", false) + " >> act=" + act + ", iid >> " + iid + "<br />");
    }

    /**
     * 推送数据错误
     *
     * @param baseData    推送内容
     * @param description 错误描述
     */
    @Override
    public void pushFail(BaseData baseData, String description) {
        sendToMessage(MainUtil.getFont("pushFail()", true) + " >> " + baseData.toString() + " >> description=" + description + "<br />");
    }

    /**
     * 已订阅
     */
    @Override
    public void subscribed(String topic) {
        sendToMessage(MainUtil.getFont("subscriptionComplete()", false) + " topic >> " + topic + "<br />");
    }

    /**
     * 订阅失败
     *
     * @param description 错误描述
     */
    @Override
    public void subscribeFail(String topic, String description) {
        sendToMessage(MainUtil.getFont("subscriptionFail()", true) + ", topic >> " + topic + " >> description=" + description + "<br />");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnClear:
                tvResult.setText("");
                tvResult.scrollTo(0, 0);
                break;
            case R.id.btnConn:
                XLink.connect(MainAty.this, getInitParams(), MainAty.this);
                break;
            case R.id.btnSeHearbeat:
                if (isHeartbeat) {
                    isHeartbeat = false;
                    btnSeHearbeat.setText("开启心跳");
                } else {
                    isHeartbeat = true;
                    btnSeHearbeat.setText("关闭心跳");
                }
                break;
            case R.id.btnDisConn:
                XLink.disConnect();
                break;
            case R.id.btnAddDev:
                //检查是否存在
                XLinkHttp.addProdId(getInitParams(), new IHttpRequest() {
                    @Override
                    public void requestComplete(String jsonData) {
                        if (!TextUtils.isEmpty(jsonData)) {
                            jsonData = "<br />" + JsonFormat.formatJson(jsonData).replace("\n", "<br />");
                        }
                        sendToMessage(MainUtil.getFont("addProdId >> requestComplete()", false) + " >> success " + jsonData + "<br />");
                    }

                    @Override
                    public void requestErr(String description) {
                        sendToMessage(MainUtil.getFont("addProdId >> requestErr()", true) + " >> description = " + description + "<br />");
                    }
                });
                break;
            case R.id.btnGetDevice://得到设备信息
                XLinkHttp.getDeviceInfo(getInitParams(), new IHttpRequest() {
                    @Override
                    public void requestComplete(String jsonData) {
                        if (!TextUtils.isEmpty(jsonData)) {
                            jsonData = "<br />" + JsonFormat.formatJson(jsonData).replace("\n", "<br />");
                        }
                        sendToMessage(MainUtil.getFont("getDeviceInfo >> requestComplete()", false) + " >> success " + jsonData + "<br />");
                    }

                    @Override
                    public void requestErr(String description) {
                        sendToMessage(MainUtil.getFont("getDeviceInfo >> requestErr()", true) + " >> requestErr description = " + description + "<br />");
                    }
                });
                break;
            case R.id.btnGetAgent://得到代理服务器列表
                XLinkHttp.getAgentList(getInitParams(), new IHttpRequest() {
                    @Override
                    public void requestComplete(String jsonData) {
                        if (!TextUtils.isEmpty(jsonData)) {
                            jsonData = "<br />" + JsonFormat.formatJson(jsonData).replace("\n", "<br />");
                        }
                        sendToMessage(MainUtil.getFont("getAgentList >> requestComplete()", false) + " >> success " + jsonData + "<br />");
                    }

                    @Override
                    public void requestErr(String description) {
                        sendToMessage(MainUtil.getFont("getAgentList >> requestErr()", true) + " >> requestErr description = " + description + "<br />");
                    }
                });
                break;
        }
    }

    static class TimerThread extends Thread {
        Handler handler;

        public TimerThread(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            while (true) {
                if (isHeartbeat) {
                    //XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "Heartbeat", null);
                    Map uploadList = new HashMap();
                    uploadList.put("15", false);
                    uploadList.put("13", false);
                    XLink.putCmd(PutType.UPLOAD, DataTransfer.createIID(), "16", uploadList);
                    //Map uploadList2 = new HashMap();
                    //uploadList2.put("0", true);
                    //XLink.putCmd(PutType.UPLOAD, DataTransfer.createIID(), "15", uploadList2);
                    //Map eventMaps = new HashMap();
                    //eventMaps.put("result", true);
                    //eventMaps.put("sex", "1");
                    //XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "hdev_rsrc_monitor", eventMaps);
                    //XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "apk_install_result", eventMaps);
                    //XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "apk_uninstall_result", eventMaps);
                    //XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "file_download_percent", eventMaps);
                    //XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "firmware_install_result", eventMaps);
                    //XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "file_upload_result", eventMaps);
                    //XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "hdev_file_occupancy", eventMaps);

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x100:
                    showData(msg.obj.toString());
                    break;
                case 0x101:
                    tvConnStatus.setText("当前连接状态：" + msg.obj.toString());
                    break;
            }
        }
    };

    public void sendToMessage(String obj) {
        handler.sendMessage(handler.obtainMessage(0x100, obj));
    }

    /**
     * 显示数据到UI
     *
     * @param htmlStr
     */
    public void showData(String htmlStr) {
        tvResult.append(Html.fromHtml(MainUtil.getTodayDateHms("yy-MM-dd HH:mm:ss") + "：" + htmlStr));
        tvResult.append("\n");
        //刷新最新行显示
        int offset = tvResult.getLineCount() * tvResult.getLineHeight();
        int tvHeight = tvResult.getHeight();
        if (offset > 6000) {
            tvResult.setText("");
            tvResult.scrollTo(0, 0);
        } else {
            if (offset > tvHeight) {
                //Log.d(TAG, "showData: offset >> " + offset + " ,tvHeight >> " + tvHeight);
                tvResult.scrollTo(0, offset - tvHeight);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        XLink.disConnect();
    }
}