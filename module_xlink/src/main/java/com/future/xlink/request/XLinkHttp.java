package com.future.xlink.request;

import android.text.TextUtils;

import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.other.Agents;
import com.future.xlink.bean.other.Body;
import com.future.xlink.bean.other.Payload;
import com.future.xlink.bean.other.ProductInfo;
import com.future.xlink.bean.other.Register;
import com.future.xlink.request.retrofit.IApis;
import com.future.xlink.callback.IHttpRequest;
import com.future.xlink.bean.base.BaseResponse;
import com.future.xlink.request.retrofit.RetrofitClient;
import com.future.xlink.request.retrofit.SelfObserver;
import com.future.xlink.utils.ERR;
import com.future.xlink.utils.GsonTools;
import com.future.xlink.utils.Utils;
import com.library.logtools.FaceLogTools;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class XLinkHttp {
    /**
     * get agent list
     */
    public static void getAgentList(InitParams params, IHttpRequest iReq) {
        String time = String.valueOf(System.currentTimeMillis());
        String token = DataTransfer.getToken(params.appKey, params.appSecret, params.clientId,
                time);
        if (token != null) {
            RetrofitClient.getInstance().getAgentList(params.httpUrl
                                    + ":" + params.httpPort + "/" + IApis.AGENT_SERVER_LIST,
                            token, time
                            , params.clientId)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new SelfObserver<BaseResponse<Agents>>() {
                        @Override
                        public void onNext(BaseResponse<Agents> bResponse) {
                            super.onNext(bResponse);
                            FaceLogTools.debug("HTTP_AGENT_LIST_RESPONSE status=" + bResponse.status + " desc=" + bResponse.description + " thread=" + Thread.currentThread().getName());
                            if (bResponse.isSuccessNonNull()) {
                                //获取服务器列表成功，进行ping操作，获得最佳连接链路
                                FaceLogTools.debug("HTTP_AGENT_LIST_SERVERS count=" + (bResponse.payload.servers == null ? 0 : bResponse.payload.servers.size()));
                                if (bResponse.payload.servers == null || bResponse.payload.servers.size() == 0) {
                                    //返回失败1
                                    iReq.requestErr(ERR.AGENTNULL,bResponse.description);
                                } else {
                                    iReq.requestComplete(GsonTools.toJsonWtihNullField(bResponse.payload));
                                }
                            } else {
                                iReq.requestErr(ERR.UNKNOWN,bResponse.description);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            super.onError(e);
                            iReq.requestErr(getErrCode(e),e.getMessage());
                        }
                    });
        } else {
            iReq.requestErr(ERR.TOKENNULL,"getAgentList:token is null");
        }
    }


    /**
     * Get device information
     */
    public static void getDeviceInfo(InitParams params, IHttpRequest iReq) {
        Map<String, Object> body = new HashMap<>();
        body.put("ack", "dev/" + params.clientId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("did", params.clientId);
        payload.put("new", true);
        payload.put("pdid", params.pdid);
        body.put("payload", payload);

        String time = String.valueOf(System.currentTimeMillis());
        String token = DataTransfer.getToken(params.uniqueKey, params.uniqueScret,
                params.clientId, time);
        if (token != null) {
            RetrofitClient.getInstance()
                    .getDeviceInfo(params.httpUrl + ":" + params.httpPort + "/" + IApis.PRODUCT_DEVICE_INFO, token, time, params.clientId, body)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new SelfObserver<BaseResponse<ProductInfo>>() {
                        @Override
                        public void onNext(BaseResponse<ProductInfo> baseResponse) {
                            super.onNext(baseResponse);
                            FaceLogTools.debug("HTTP_DEVICE_INFO_RESPONSE status=" + baseResponse.status + " desc=" + baseResponse.description + " thread=" + Thread.currentThread().getName());
                            if (baseResponse.status == 0) {
                                //产品一致
                                iReq.requestComplete(GsonTools.toJsonWtihNullField(baseResponse.payload));
                            } else if (baseResponse.status == 1) {
                                if (baseResponse.payload == null) {
                                    //设备不存在
                                    iReq.requestErr(ERR.DEVNOTEXIST,"设备不存在！");
                                } else {
                                    //产品不一致
                                    iReq.requestErr(ERR.PRODUCTDIFF,"产品不一致！");
                                }
                            } else {
                                iReq.requestErr(ERR.UNKNOWN,"检查参数,系统时间是否正常！");
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            super.onError(e);
                            iReq.requestErr(getErrCode(e),e.getMessage());
                        }
                    });
        } else {
            iReq.requestErr(ERR.TOKENNULL,"getDeviceInfo:token is null");
        }
    }

    /**
     * add sn to iot
     */
    public static void addProdId(InitParams params, IHttpRequest iReq) {
        Map<String, Object> body = new HashMap<>();
        body.put("ack", "dev/" + params.clientId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("did", params.clientId);
        payload.put("new", true);
        payload.put("pdid", params.pdid);
        String time = String.valueOf(System.currentTimeMillis());
        body.put("payload", payload);
        String token = DataTransfer.getToken(params.uniqueKey, params.uniqueScret,
                params.clientId, time);
        if (token != null) {
            RetrofitClient.getInstance()
                    .uniqueProduct(params.httpUrl + ":" + params.httpPort + "/" + IApis.PRODUCT_UNIQUE, token, time, params.clientId, body)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new SelfObserver<BaseResponse>() {
                        @Override
                        public void onNext(BaseResponse baseResponse) {
                            super.onNext(baseResponse);
                            FaceLogTools.debug("HTTP_ADD_DEVICE_RESPONSE status=" + baseResponse.status + " desc=" + baseResponse.description + " thread=" + Thread.currentThread().getName());
                            if (baseResponse.status == 0) {
                                iReq.requestComplete("{\"code\":0,\"description\":\"已完成\"}");
                            } else {
                                if (!Utils.isEmpty(baseResponse.description)
                                        && baseResponse.description.indexOf("SN已经存在") != -1) {
                                    iReq.requestComplete("{\"code\":0,\"description\":\"已完成\"}");
                                } else {
                                    iReq.requestErr(ERR.UNKNOWN,baseResponse.description);
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            super.onError(e);
                            iReq.requestErr(getErrCode(e),e.getMessage());
                        }
                    });
        } else {
            iReq.requestErr(ERR.TOKENNULL,"addProdId:token is null");
        }
    }

    public static int getErrCode(Throwable e) {
        FaceLogTools.error("HTTP_ERROR type=" + e.getClass().getSimpleName() + " error=" + e.getMessage());
        if (e instanceof SocketTimeoutException) {
            return ERR.READTIMEOUT;
        } else if (e instanceof UnknownHostException) {
            return ERR.UNKNOWNHOST;
        } else {
            return ERR.UNKNOWN;
        }
    }

    /**
     * register dev
     */
    public static void registerDev(InitParams params, String url, IHttpRequest iReq) {
        Body body = new Body();
        body.ack = "dev/" + params.clientId;
        Payload payload = new Payload();
        payload.did = params.clientId;
        payload.pdid = params.pdid;
        payload.oldMqttBroker = Utils.isEmpty(params.mqttBroker) ? "" : params.mqttBroker;
        payload.isNew = true;
        body.payload = payload;
        String time = String.valueOf(System.currentTimeMillis());
        String token = DataTransfer.getToken(params.appKey, params.appSecret, params.clientId,
                time);
        //FaceLogTools.debug("registerDev>>body>>"+GsonTools.toJsonWtihNullField(body));
        if (token != null) {
            RetrofitClient.getInstance()
                    .registerAgent(url + IApis.AGENT_REGISTER, token, time, params.clientId, body)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Consumer<BaseResponse<Register>>() {
                        @Override
                        public void accept(BaseResponse<Register> regPonse) throws Exception {
                            FaceLogTools.debug("HTTP_REGISTER_RESPONSE status=" + regPonse.status + " desc=" + regPonse.description + " thread=" + Thread.currentThread().getName());
                            if (regPonse.isSuccessNonNull()) {
                                iReq.requestComplete(GsonTools.toJsonWtihNullField(regPonse.payload));
                            } else {
                                iReq.requestErr(ERR.UNKNOWN,regPonse.description);
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable e) throws Exception {
                            iReq.requestErr(getErrCode(e),e.getMessage());
                        }
                    });
        } else {
            iReq.requestErr(ERR.TOKENNULL,"registerDev:token is null");
        }
    }
}
