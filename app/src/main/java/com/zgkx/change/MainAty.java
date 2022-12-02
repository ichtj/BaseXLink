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
import com.future.xlink.utils.PingUtils;
import com.future.xlink.utils.Utils;
import com.future.xlink.xlog.XLogTools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class MainAty extends Activity implements IMqttCallback, View.OnClickListener {
    private static final String TAG = MainAty.class.getSimpleName();
    Button btnSeHearbeat;
    Button btnGetAgent;
    Button btnGetDevice;
    Button btnClear;
    Button btnAddDev;
    Button btnConn;
    Button btnDisConn;
    TextView tvResult;
    static boolean isHeartbeat = false;
    String clientId = "FSMMMNNNFF0";
    TimerThread timerThread;

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
        String configFolder = IApis.ROOT + getPackageName() + "/" + clientId + "/xlink-log/";
        XLogTools.initResetXLog(configFolder);
        timerThread=new TimerThread();
        timerThread.start();
    }


    @Override
    public void connChange(boolean connected, String description) {
        sendToMessage(MainUtil.getFont("connChange()", !connected) + " >> connected=" + connected + ",description=" + description + "<br />");
        if (connected) {
            XLink.subscribe("dev/" + clientId + "/#", 2);
            XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "online", null);
        }
    }

    @Override
    public void iotRequest(BaseData baseData) {
        sendToMessage(MainUtil.getFont("iotRequest()", false) + " >> " + baseData.toString() + "<br />");
        if (baseData.operation.equals("remote_cmd")) {
            Map<String, Object> cmdList = new HashMap<>();
            cmdList.put("data", "这是一个测试data");
            cmdList.put("result", "这是一个测试result");
            cmdList.put("description", "这是一个测试description");
            XLink.putCmd(PutType.METHOD, baseData.iid, baseData.operation, cmdList);
        }
    }

    @Override
    public void iotReply(String act, String iid) {
        sendToMessage(MainUtil.getFont("iotReply()", false) + " >> act=" + act + ", iid >> " + iid + "<br />");
    }

    @Override
    public void pushComplete(BaseData baseData) {
        sendToMessage(MainUtil.getFont("pushComplete()", false) + " >> " + baseData.toString() + "<br />");
    }

    @Override
    public void pushFail(String description) {
        sendToMessage(MainUtil.getFont("pushFail()", true) + " >> description=" + description + "<br />");
    }

    @Override
    public void subscribeComplete() {
        sendToMessage(MainUtil.getFont("subscriptionComplete()", false) + " >> " + "<br />");
    }

    @Override
    public void subscribeFail(String description) {
        sendToMessage(MainUtil.getFont("subscriptionFail()", true) + " >> description=" + description + "<br />");
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnClear:
                tvResult.setText("");
                break;
            case R.id.btnConn:
                XLink.connect(MainAty.this, getInitParams(), MainAty.this);
                break;
            case R.id.btnSeHearbeat:
                if (!XLink.getConnectStatus()) {
                    Toast.makeText(this, "请先进行连接！", Toast.LENGTH_SHORT).show();
                    return;
                }
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

    static class TimerThread extends Thread{
        @Override
        public void run() {
            while (true) {
                if(isHeartbeat&&XLink.getConnectStatus()){
                    XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "Heartbeat", null);
                    Map uploadList=new HashMap();
                    uploadList.put("15",true);
                    XLink.putCmd(PutType.UPLOAD, DataTransfer.createIID(), "16", uploadList);
                    try {
                        Thread.sleep(2000);
                    }catch (Throwable e){
                    }
                }
            }
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            showData(msg.obj.toString());

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
        tvResult.append(Html.fromHtml(MainUtil.getTodayDateHms("yyMMdd HH:mm:ss") + "：" + htmlStr));
        //刷新最新行显示
        int offset = tvResult.getLineCount() * tvResult.getLineHeight();
        if (offset > tvResult.getHeight()) {
            tvResult.scrollTo(0, offset - tvResult.getHeight());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}