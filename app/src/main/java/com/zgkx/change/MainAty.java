package com.zgkx.change;

import android.app.Activity;
import android.content.Intent;
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

import java.io.IOException;
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
    private Button btnUninit;
    private Button btnDisConn;
    private Button btnAutoConn;
    private Button btnPushEvent;
    private Button btnReboot;
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
        btnReboot = findViewById(R.id.btnReboot);
        btnAutoConn = findViewById(R.id.btnAutoConn);
        boolean isAutoConn = SPUtils.getBoolean(this, MainUtil.KEY_AUTOCONN, false);
        btnAutoConn.setText("开机自启：" + isAutoConn);
        tvConnStatus = findViewById(R.id.tvConnStatus);
        tvSn = findViewById(R.id.tvSn);
        btnPushEvent = findViewById(R.id.btnPushEvent);
        btnPushEvent.setOnClickListener(this);
        btnSeHearbeat = findViewById(R.id.btnSeHearbeat);
        btnGetAgent = findViewById(R.id.btnGetAgent);
        btnGetDevice = findViewById(R.id.btnGetDevice);
        btnClear = findViewById(R.id.btnClear);
        btnAddDev = findViewById(R.id.btnAddDev);
        btnDisConn = findViewById(R.id.btnDisConn);
        tvResult = findViewById(R.id.tvResult);
        tvResult.setMovementMethod(ScrollingMovementMethod.getInstance());
        btnUninit = findViewById(R.id.btnUninit);
        btnUninit.setOnClickListener(this);
        btnConn = findViewById(R.id.btnConn);
        btnClear.setOnClickListener(this);
        btnAddDev.setOnClickListener(this);
        btnConn.setOnClickListener(this);
        btnDisConn.setOnClickListener(this);
        btnGetDevice.setOnClickListener(this);
        btnGetAgent.setOnClickListener(this);
        btnSeHearbeat.setOnClickListener(this);
        btnAutoConn.setOnClickListener(this);
        btnReboot.setOnClickListener(this);
        tvSn.setText("当前操作的SN：" + clientId);

        String configFolder = IApis.ROOT + getPackageName() + "/" + clientId + "/xlink-log/";
        XLogTools.initXLog(configFolder);
        timerThread = new TimerThread(handler);
        timerThread.start();

        if (isAutoConn) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btnConn.performClick();
                    try {
                        Thread.sleep(1500);
                    } catch (Throwable e) {
                        Log.e(TAG, "run: ", e);
                    }
                    btnSeHearbeat.performClick();
                }
            }, 5000);
        }
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
            Map<String, Object> maps = new HashMap<>();
            maps.put("address", "飞思未来深圳科技有限公司");
            maps.put("longitude", "113°55′43.91″");
            maps.put("latitude", "22°37′55.12″");
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
        switch (baseData.iPutType) {
            case PutType.UPGRADE:
                Map<String, Object> upMaps = new HashMap<>();
                upMaps.put("startTime", "2022-11-11");
                upMaps.put("endTime", "2022-11-12");
                upMaps.put("description", "这是一个测试upgrade");
                XLink.putCmd(PutType.UPGRADE, baseData.iid, baseData.operation, upMaps);
                break;
            case PutType.METHOD:
                Map<String, Object> methodMaps = new HashMap<>();
                methodMaps.put("data", "这是一个测试data");
                methodMaps.put("result", "这是一个测试result");
                methodMaps.put("description", "这是一个测试description");
                XLink.putCmd(PutType.METHOD, baseData.iid, baseData.operation, methodMaps);
                break;
            case PutType.GETPERTIES:
                Map<String, Object> getMaps = new HashMap<>();
                getMaps.put("prid", baseData.maps.get("prid").toString());
                getMaps.put("value", true);
                XLink.putCmd(PutType.GETPERTIES, baseData.iid, baseData.operation, getMaps);
                break;
            case PutType.SETPERTIES:
                XLink.putCmd(PutType.SETPERTIES, baseData.iid, baseData.operation, baseData.maps);
                break;
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
            case R.id.btnUninit:
                XLink.unInit(MainAty.this);
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
                    public void requestErr(int errCode,String description) {
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
                    public void requestErr(int errCode,String description) {
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
                    public void requestErr(int errCode,String description) {
                        sendToMessage(MainUtil.getFont("getAgentList >> requestErr()", true) + " >> requestErr description = " + description + "<br />");
                    }
                });
                break;
            case R.id.btnAutoConn:
                boolean isAutoConn = SPUtils.getBoolean(this, MainUtil.KEY_AUTOCONN, false);
                btnAutoConn.setText("开机自启：" + !isAutoConn);
                SPUtils.putBoolean(this, MainUtil.KEY_AUTOCONN, !isAutoConn);
                break;
            case R.id.btnReboot:
                //调用系统接口进行重启
                try {
                    Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
                } catch (Throwable e) {
                }
                break;
            case R.id.btnPushEvent:
                if(XLink.getConnectStatus()){
                    MainUtil.pushTestEvent();
                    MainUtil.pushTestEvent2();
                }else{
                    Toast.makeText(this,"请先连接",Toast.LENGTH_LONG).show();
                }
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
                    XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "Heartbeat", null);

                    Map uploadList1 = new HashMap();
                    uploadList1.put("prid", "15");
                    uploadList1.put("value", false);
                    XLink.putCmd(PutType.UPLOAD, DataTransfer.createIID(), "16", uploadList1);

                    Map uploadList2 = new HashMap();
                    uploadList2.put("prid", "13");
                    uploadList2.put("value", false);
                    XLink.putCmd(PutType.UPLOAD, DataTransfer.createIID(), "16", uploadList2);


                    MainUtil.pushTestEvent();
                    MainUtil.pushTestEvent2();
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