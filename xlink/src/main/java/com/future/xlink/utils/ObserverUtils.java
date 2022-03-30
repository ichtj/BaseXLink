package com.future.xlink.utils;

import android.content.Context;
import android.os.Environment;

import com.elvishew.xlog.XLog;
import com.future.xlink.api.ApiService;
import com.future.xlink.api.response.BaseResponse;
import com.future.xlink.api.retrofit.RetrofitClient;
import com.future.xlink.api.subscribe.SelfObserver;
import com.future.xlink.bean.Agents;
import com.future.xlink.bean.Constants;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.LogBean;
import com.future.xlink.bean.LogPayload;
import com.future.xlink.bean.Register;
import com.future.xlink.bean.common.InitState;
import com.future.xlink.bean.request.Body;
import com.future.xlink.bean.request.Payload;
import com.future.xlink.listener.PingListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ObserverUtils {
    private static final String TAG = "ObserverUtils";

    public static void getUploadLogUrl(Context context, InitParams params, LogBean bean) {
        String time = String.valueOf(System.currentTimeMillis());
        RetrofitClient.getInstance().getUploadLogUrl(params.httpServer + GlobalConfig.UPLOAD_LOGURL, Utils.getToken(params, time), time, params.sn, bean)
                .subscribeOn(Schedulers.io()).subscribe(new SelfObserver<BaseResponse<LogPayload>>() {
            @Override
            public void onNext(BaseResponse<LogPayload> baseResponse) {
                super.onNext(baseResponse);
                XLog.d("getUploadLogUrl "+GsonUtils.toJsonWtihNullField(baseResponse));
                if (baseResponse.status == 0) {
                    //日志上传接口请求成功，上传文件
                    String path = Environment.getExternalStorageDirectory().getPath() + File.separator + bean.filename;
                    File file = new File(path);
                    XLog.d("getUploadLogUrl size==" + file.length());
                    doUploadFile(file, baseResponse.payload, new Callback<BaseResponse>() {
                        @Override
                        public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                            XLog.d("onResponse result==>" + response.isSuccessful());
                        }

                        @Override
                        public void onFailure(Call<BaseResponse> call, Throwable t) {
                            XLog.e("getUploadLogUrl1",t);

                        }
                    });
                }
            }

            @Override
            public void onError(Throwable e) {
                super.onError(e);
                XLog.e("getUploadLogUrl2",e);
            }
        });
    }

    public static void doUploadFile(File uploadFile, LogPayload payload, Callback<BaseResponse> callback) {
        Map<String, RequestBody> data = new HashMap<>();
        MediaType textMedia = MediaType.parse("text/plain");
        for (Map.Entry<String, String> pair : payload.formArgs.entrySet()) {
            data.put(pair.getKey(), RequestBody.create(textMedia, pair.getValue()));
        }
        RequestBody file = RequestBody.create(MediaType.parse("application/octet-stream"), uploadFile);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(payload.fileFormDataName, payload.fileFormDataFilename, file);
    }

    /**
     * 获取代理服务地址
     *
     * @param context 上下文
     * @param params 必要参数
     */
    public static void getAgentList(Context context, InitParams params) {
        String time = String.valueOf(System.currentTimeMillis());
        String token = Utils.getToken(params, time);
        RetrofitClient.getInstance().getAgentList(params.httpServer+GlobalConfig.AGENT_SERVER_LIST, token, time, params.sn)
                .subscribeOn(Schedulers.io()).subscribe(new SelfObserver<BaseResponse<Agents>>() {
            @Override
            public void onNext(BaseResponse<Agents> baseResponse) {
                super.onNext(baseResponse);
                XLog.d("getAgentList: baseResponse="+baseResponse.toString());
                if (baseResponse.isSuccessNonNull()) {
                    //获取服务器列表成功，进行ping操作，获得最佳连接链路
                    List<String> pinglist = baseResponse.payload.servers;
                    XLog.d("onNext pinglist:"+ pinglist.toString());
                    if (pinglist == null || pinglist.size() == 0) {
                        //返回失败1
                        XBus.post(new Carrier(Carrier.TYPE_MODE_INIT_RX, InitState.INIT_GETAGENT_FAIL));
                    } else if (pinglist.size() == 1) {
                        //如果列表只有一个取消ping操作
                        registerRequest(context, params, baseResponse.payload.servers.get(0));
                    } else {
                        //执行循环ping操作,找到最优的线路
                        ObserverUtils.pingTest(pinglist, o -> registerRequest(context, params, o));
                    }
                } else {
                    if (baseResponse.description.equals("设备不存在")) {
                        XBus.post(new Carrier(Carrier.TYPE_MODE_INIT_RX, InitState.INIT_DEVICE_NOT_EIXST_ERR));
                    } else {
                        //获取代理服务器列表失败2
                        XBus.post(new Carrier(Carrier.TYPE_MODE_INIT_RX, InitState.INIT_GETAGENT_FAIL));
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                super.onError(e);
                XBus.post(new Carrier(Carrier.TYPE_MODE_INIT_RX, InitState.INIT_GETAGENT_ERR));
                XLog.e("getAgentList", e);
            }
        });
    }

    private static void registerRequest(Context context, InitParams params, String url) {
        Body body = new Body();
        body.ack = "dev/" + params.sn;
        Payload payload = new Payload();
        payload.did = params.sn;
        payload.pdid = params.pdid;
        Register register=params.register;
        payload.oldMqttBroker = register==null?"":register.mqttBroker;
        payload.isNew = true;
        body.payload = payload;
        String time = String.valueOf(System.currentTimeMillis());
        RetrofitClient.getInstance().registerAgent(url + GlobalConfig.AGENT_REGISTER, Utils.getToken(params, time), time, params.sn, body)
                .subscribeOn(Schedulers.io()).subscribe(new SelfObserver<BaseResponse<Register>>() {
            @Override
            public void onNext(BaseResponse<Register> registerBaseResponse) {
                super.onNext(registerBaseResponse);
                XLog.d("registerRequest onNext status:" + registerBaseResponse.status);
                if (registerBaseResponse.isSuccess() && registerBaseResponse.isSuccessNonNull()) {
                    params.register=registerBaseResponse.payload;
                    XLog.d("registerRequest onNext get register:" + params.register.toString());
                    if (!params.register.isNull()) {
                        XBus.post(new Carrier(Carrier.TYPE_MODE_INIT_RX, InitState.INIT_SUCCESS));
                    } else {
                        XBus.post(new Carrier(Carrier.TYPE_MODE_INIT_RX, InitState.INIT_CACHE_NOEXIST));
                    }
                } else {
                    XBus.post(new Carrier(Carrier.TYPE_MODE_INIT_RX, InitState.INIT_REGISTER_AGENT_FAIL));
                }
            }

            @Override
            public void onError(Throwable e) {
                XLog.e("registerRequest",e);
                XBus.post(new Carrier(Carrier.TYPE_MODE_INIT_RX, InitState.INIT_REGISTER_AGENT_ERR));
            }
        });
    }


    public static void pingTest(List<String> pingUrl, PingListener listener) {
        final AtomicInteger batch = new AtomicInteger(0);
        int threadNum = pingUrl.size();
        final ExecutorService executor = Executors.newFixedThreadPool(threadNum);
        List<String> list = new ArrayList<>();
        final Scheduler scheduler = Schedulers.from(executor);
        Observable.range(0, pingUrl.size())
                .groupBy(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(@NonNull Integer integer) throws Exception {
                        return batch.getAndDecrement();
                    }
                })
                .flatMap(new Function<GroupedObservable<Integer, Integer>, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(@NonNull GroupedObservable<Integer, Integer> integerIntegerGroupedObservable) throws Exception {
                        return integerIntegerGroupedObservable.observeOn(scheduler)
                                .map(new Function<Integer, String>() {

                                    @Override
                                    public String apply(@NonNull Integer integer) throws Exception {
                                        return PingUtils.ping(pingUrl.get(integer.intValue()), 3, new StringBuffer());
                                    }
                                });
                    }
                }).doFinally(new Action() {
            @Override
            public void run() throws Exception {
                executor.shutdown();
                System.out.println("ping --end");
                //ping结果比较，获取最佳的链接
                if (listener != null) {
                    listener.pingResult(compare(list));
                }

            }
        })
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        list.add((String) o);
                    }
                });
    }

    /**
     * 比较获取最优的服务端链接
     */
    private static String compare(List<String> list) {
        Collections.sort(list, (s1, s2) -> {
            String[] o1 = s1.split("#");
            String[] o2 = s2.split("#");
            if (o1.length > o2.length) {
                return -1;
            } else if (o1.length < o2.length) {
                return 1;
            } else if ((o1.length == o2.length) && (o1.length == 1)) {
                return 1;
            } else {
                return Float.parseFloat(o1[1]) >= Float.parseFloat(o2[1]) ? 1 : -1;
            }

        });
        return list.get(0).split("#")[0];
    }

}
