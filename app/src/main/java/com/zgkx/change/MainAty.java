package com.zgkx.change;

import android.app.Activity;
import android.content.Intent;
import android.net.InetAddresses;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.future.xlink.utils.JsonFormat;
import com.future.xlink.utils.Utils;
import com.future.xlink.xlog.XLogTools;
import com.library.logtools.FaceLogTools;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainAty extends Activity implements IMqttCallback, View.OnClickListener {
    private static final String TAG = MainAty.class.getSimpleName() + "F";
    private Button btnSeHearbeat;
    private Button btnGetAgent;
    private Button btnGetDevice;
    private Button btnClear;
    private Button btnClearMQttCache;
    private Button btnAddDev;
    private Button btnConn;
    private Button btnUninit;
    private Button btnDisConn;
    private Button btnAutoConn;
    private Button btnPushEvent;
    private Button btnReboot;
    private TextView tvResult;
    private TextView tvConnStatus;
    private EditText etSn;
    private static volatile boolean isHeartbeat = false;
    private boolean lastConnected = false;
    //private String clientId = "FSMMMNNNFF001";
    private String clientId;
    // Periodic heartbeat uses a single worker to avoid overlapping sends on slow devices.
    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    // Button actions may touch MQTT/network state, so keep them off the main thread.
    private final ExecutorService actionExecutor = Executors.newSingleThreadExecutor();

    /**
     * 获取初始化参数
     *
     * @return 初始化对象
     */
    public InitParams getInitParams() {
        InitParams iParam = new InitParams();
        iParam.autoReConnect = true;
        iParam.cleanSession = true;
        iParam.clientId = clientId;
        iParam.qos = 0;
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
        etSn = findViewById(R.id.etSn);
        btnPushEvent = findViewById(R.id.btnPushEvent);
        btnPushEvent.setOnClickListener(this);
        btnSeHearbeat = findViewById(R.id.btnSeHearbeat);
        btnGetAgent = findViewById(R.id.btnGetAgent);
        btnGetDevice = findViewById(R.id.btnGetDevice);
        btnClearMQttCache = findViewById(R.id.btnClearMQttCache);
        btnClear = findViewById(R.id.btnClear);
        btnAddDev = findViewById(R.id.btnAddDev);
        btnDisConn = findViewById(R.id.btnDisConn);
        tvResult = findViewById(R.id.tvResult);
        tvResult.setMovementMethod(ScrollingMovementMethod.getInstance());
        btnUninit = findViewById(R.id.btnUninit);
        btnUninit.setOnClickListener(this);
        btnConn = findViewById(R.id.btnConn);
        btnClearMQttCache.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnAddDev.setOnClickListener(this);
        btnConn.setOnClickListener(this);
        btnDisConn.setOnClickListener(this);
        btnGetDevice.setOnClickListener(this);
        btnGetAgent.setOnClickListener(this);
        btnSeHearbeat.setOnClickListener(this);
        btnAutoConn.setOnClickListener(this);
        btnReboot.setOnClickListener(this);
        FormatViewUtils.setMovementMethod(tvResult);

        String configFolder = new File(getFilesDir(), "xlink-log").getAbsolutePath() + File.separator;
        XLogTools.initXLog(this,configFolder);
        service.scheduleAtFixedRate(this::run, 0, 15, TimeUnit.SECONDS);

        if (isAutoConn) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btnConn.performClick();
                    handler.postDelayed(() -> btnSeHearbeat.performClick(), 1500);
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
        Log.d(TAG, "UI_CONN_STATE connected=" + connected + " desc=" + description);
        sendToMessage(MainUtil.getFont("connState", !connected) + " connected=" + connected + " desc=" + description + "<br />");
        boolean firstConnected = connected && !lastConnected;
        lastConnected = connected;
        if (firstConnected) {
            XLink.subscribe("dev/" + clientId + "/#");
            Map<String, Object> maps = new HashMap<>();
            maps.put("address", "飞思未来深圳科技有限公司");
            maps.put("longitude", "113°55′43.91″");
            maps.put("latitude", "22°37′55.12″");
            XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "online", maps);
            // If the user enabled heartbeat before reconnecting, send one immediately
            // after MQTT is ready instead of waiting for the next scheduled tick.
            if (isHeartbeat) {
                runOffMain("heartbeatOnConnect", this::pushHeartbeatOnce);
            }
        } else if (connected) {
            Log.d(TAG, "UI_CONN_DUP_IGNORE connected=true");
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
        Log.d(TAG, "UI_MSG_ARRIVE type=" + baseData.iPutType + " iid=" + baseData.iid + " op=" + baseData.operation);
        sendToMessage(MainUtil.getFont("msgArrives", false) + " type=" + baseData.iPutType + " iid=" + baseData.iid + " op=" + baseData.operation + "<br />");
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
        sendToMessage(MainUtil.getFont("pushed", false) + " iid=" + baseData.iid + " op=" + baseData.operation + "<br />");
    }

    /**
     * 平台回复消息
     *
     * @param act
     * @param iid
     */
    @Override
    public void iotReplyed(String act, String iid) {
        Log.d(TAG, "UI_REPLY act=" + act + " iid=" + iid);
        sendToMessage(MainUtil.getFont("iotReplyed", false) + " act=" + act + " iid=" + iid + "<br />");
    }

    /**
     * 推送数据错误
     *
     * @param baseData    推送内容
     * @param description 错误描述
     */
    @Override
    public void pushFail(BaseData baseData, String description) {
        Log.d(TAG, "UI_PUSH_FAIL iid=" + baseData.iid + " op=" + baseData.operation + " desc=" + description);
        sendToMessage(MainUtil.getFont("pushFail", true) + " iid=" + baseData.iid + " op=" + baseData.operation + " desc=" + description + "<br />");
    }

    /**
     * 已订阅
     */
    @Override
    public void subscribed(String topic) {
        Log.d(TAG, "UI_SUBSCRIBE_OK topic=" + topic);
        sendToMessage(MainUtil.getFont("subscriptionComplete", false) + " topic=" + topic + "<br />");
    }

    /**
     * 订阅失败
     *
     * @param description 错误描述
     */
    @Override
    public void subscribeFail(String topic, String description) {
        Log.d(TAG, "UI_SUBSCRIBE_FAIL topic=" + topic + " desc=" + description);
        sendToMessage(MainUtil.getFont("subscriptionFail", true) + " topic=" + topic + " desc=" + description + "<br />");
    }

    @Override
    public void onClick(View v) {
        if (checkSn()) return;
        String actionName = getResources().getResourceEntryName(v.getId());
        Log.d(TAG, "UI_CLICK action=" + actionName + " connected=" + XLink.getConnectStatus() + " heartbeat=" + isHeartbeat);
        sendToMessage(MainUtil.getFont("click", false) + " action=" + actionName + "<br />");
        switch (v.getId()) {
            case R.id.btnClearMQttCache:
                runOffMain(actionName, () -> XLink.clearMqttDatabaseFiles(MainAty.this));
                break;
            case R.id.btnClear:
                tvResult.setText("");
                tvResult.scrollTo(0, 0);
                break;
            case R.id.btnConn:
                runOffMain(actionName, () -> XLink.connect(MainAty.this, getInitParams(), MainAty.this));
                break;
            case R.id.btnUninit:
                runOffMain(actionName, () -> XLink.unInit(MainAty.this));
                break;
            case R.id.btnSeHearbeat:
                if (isHeartbeat) {
                    isHeartbeat = false;
                    btnSeHearbeat.setText("开启心跳");
                } else {
                    isHeartbeat = true;
                    if (XLink.getConnectStatus()) {
                        runOffMain("heartbeatOnce", this::pushHeartbeatOnce);
                    } else {
                        Log.d(TAG, "HEARTBEAT_WAIT_CONNECT enabled=true connected=false");
                        sendToMessage(MainUtil.getFont("heartbeat", true) + " waitingForConnect=true<br />");
                    }
                    btnSeHearbeat.setText("关闭心跳");
                }
                break;
            case R.id.btnDisConn:
                FormatViewUtils.scrollBackToTop(tvResult);
                runOffMain(actionName, XLink::disConnect);
                break;
            case R.id.btnAddDev:
                //检查是否存在
                XLinkHttp.addProdId(getInitParams(), new IHttpRequest() {
                    @Override
                    public void requestComplete(String jsonData) {
                        if (!Utils.isEmpty(jsonData)) {
                            jsonData = "<br />" + JsonFormat.formatJson(jsonData).replace("\n", "<br />");
                        }
                        Log.d(TAG, "HTTP_ADD_DEVICE_OK hasBody=" + !Utils.isEmpty(jsonData));
                        sendToMessage(MainUtil.getFont("httpAddDevice", false) + " success=true body=" + jsonData + "<br />");
                    }

                    @Override
                    public void requestErr(int errCode, String description) {
                        Log.d(TAG, "HTTP_ADD_DEVICE_FAIL code=" + errCode + " desc=" + description);
                        sendToMessage(MainUtil.getFont("httpAddDevice", true) + " success=false code=" + errCode + " desc=" + description + "<br />");
                    }
                });
                break;
            case R.id.btnGetDevice://得到设备信息
                XLinkHttp.getDeviceInfo(getInitParams(), new IHttpRequest() {
                    @Override
                    public void requestComplete(String jsonData) {
                        if (!Utils.isEmpty(jsonData)) {
                            jsonData = "<br />" + JsonFormat.formatJson(jsonData).replace("\n", "<br />");
                        }
                        Log.d(TAG, "HTTP_DEVICE_INFO_OK hasBody=" + !Utils.isEmpty(jsonData));
                        sendToMessage(MainUtil.getFont("httpDeviceInfo", false) + " success=true body=" + jsonData + "<br />");
                    }

                    @Override
                    public void requestErr(int errCode, String description) {
                        Log.d(TAG, "HTTP_DEVICE_INFO_FAIL code=" + errCode + " desc=" + description);
                        sendToMessage(MainUtil.getFont("httpDeviceInfo", true) + " success=false code=" + errCode + " desc=" + description + "<br />");
                    }
                });
                break;
            case R.id.btnGetAgent://得到代理服务器列表
                XLinkHttp.getAgentList(getInitParams(), new IHttpRequest() {
                    @Override
                    public void requestComplete(String jsonData) {
                        if (!Utils.isEmpty(jsonData)) {
                            jsonData = "<br />" + JsonFormat.formatJson(jsonData).replace("\n", "<br />");
                        }
                        Log.d(TAG, "HTTP_AGENT_LIST_OK hasBody=" + !Utils.isEmpty(jsonData));
                        sendToMessage(MainUtil.getFont("httpAgentList", false) + " success=true body=" + jsonData + "<br />");
                    }

                    @Override
                    public void requestErr(int errCode, String description) {
                        Log.d(TAG, "HTTP_AGENT_LIST_FAIL code=" + errCode + " desc=" + description);
                        sendToMessage(MainUtil.getFont("httpAgentList", true) + " success=false code=" + errCode + " desc=" + description + "<br />");
                    }
                });
                break;
            case R.id.btnAutoConn:
                boolean isAutoConn = SPUtils.getBoolean(this, MainUtil.KEY_AUTOCONN, false);
                btnAutoConn.setText("开机自启：" + !isAutoConn);
                SPUtils.putBoolean(this, MainUtil.KEY_AUTOCONN, !isAutoConn);
                break;
            case R.id.btnReboot:
                rebootSystemDialog();
                break;
            case R.id.btnPushEvent:
                if (XLink.getConnectStatus()) {
                    runOffMain(actionName, () -> {
                        MainUtil.pushTestEvent();
                        MainUtil.pushTestEvent2();
                    });
                } else {
                    Toast.makeText(this, "请先连接", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    /**
     * 重启弹窗确认
     */
    private void rebootSystemDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("确定要重启设备吗？")
                .setPositiveButton("确定", (dialog, which) -> runOffMain("reboot", this::rebootSystem))
                .setNegativeButton("取消", null)
                .show();
    }

    public void rebootSystem(){
        try{
            Intent intent = new Intent(Intent.ACTION_REBOOT);
            intent.putExtra("nowait", 1);
            intent.putExtra("interval", 1);
            intent.putExtra("window", 0);
            sendBroadcast(intent);
        }catch(Throwable throwable){
            Log.e(TAG, "REBOOT_BROADCAST_ERROR error=" + throwable.getMessage(), throwable);
        }
        try {
            Process process = Runtime.getRuntime().exec("su");
            java.io.DataOutputStream os = new java.io.DataOutputStream(process.getOutputStream());
            os.writeBytes("reboot\n");
            os.flush();
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Throwable throwable) {
            Log.e(TAG, "REBOOT_SHELL_ERROR error=" + throwable.getMessage(), throwable);
        }
    }

    private void runOffMain(String actionName, Runnable runnable) {
        actionExecutor.execute(() -> {
            try {
                Log.d(TAG, "TASK_START action=" + actionName);
                runnable.run();
                Log.d(TAG, "TASK_END action=" + actionName);
            } catch (Throwable throwable) {
                Log.e(TAG, "TASK_ERROR action=" + actionName + " error=" + throwable.getMessage(), throwable);
                sendToMessage(MainUtil.getFont("task", true) + " action=" + actionName + " error=" + throwable.getMessage() + "<br />");
            }
        });
    }


    private boolean checkSn() {
        clientId = etSn.getText().toString().trim();
        if (ObjectUtils.isEmpty(clientId)) {
            Toast.makeText(this, "请输入SN", Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    int count=0;
    public void run() {
        try {
            if (!isHeartbeat) {
                Log.d(TAG, "HEARTBEAT_TICK_SKIP reason=disabled");
                return;
            }
            if (!XLink.getConnectStatus()) {
                Log.d(TAG, "HEARTBEAT_TICK_SKIP reason=disconnected");
                return;
            }
            pushHeartbeatOnce();
        } catch (Throwable throwable) {
            Log.e(TAG, "HEARTBEAT_TICK_ERROR error=" + throwable.getMessage(), throwable);
            sendToMessage(MainUtil.getFont("heartbeat", true) + " error=" + throwable.getMessage() + "<br />");
        }
    }

    private synchronized void pushHeartbeatOnce() {
        int current = count++;
        // Each heartbeat round queues one event and two property uploads. The return
        // values only mean "queued locally"; publish result is logged by XLink.
        boolean eventOk = XLink.putCmd(PutType.EVENT, DataTransfer.createIID(), "Heartbeat", null);

        Map uploadList1 = new HashMap();
        uploadList1.put("prid", "15");
        uploadList1.put("value", current);
        boolean upload1Ok = XLink.putCmd(PutType.UPLOAD, DataTransfer.createIID(), "16", uploadList1);

        Map uploadList2 = new HashMap();
        uploadList2.put("prid", "13");
        uploadList2.put("value", current);
        boolean upload2Ok = XLink.putCmd(PutType.UPLOAD, DataTransfer.createIID(), "16", uploadList2);

        Log.d(TAG, "HEARTBEAT_SEND count=" + current + " queuedEvent=" + eventOk + " queuedUpload15=" + upload1Ok + " queuedUpload13=" + upload2Ok + " connected=" + XLink.getConnectStatus());
        sendToMessage(MainUtil.getFont("heartbeat", !(eventOk && upload1Ok && upload2Ok)) + " count=" + current + " queuedEvent=" + eventOk + " queuedUpload15=" + upload1Ok + " queuedUpload13=" + upload2Ok + "<br />");
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
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
        FormatViewUtils.formatData(tvResult, htmlStr, "yyyy-MM-dd HH:mm:ss");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        service.shutdownNow();
        actionExecutor.shutdownNow();
        new Thread(XLink::disConnect, "XLink-Disconnect").start();
    }
}
