package com.future.xlink.utils;

import com.future.xlink.bean.ActionFlag;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;

public class GsonUtils {
    private static Gson filterNullGson;
    private static Gson nullableGson;
    static {
        nullableGson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .setDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
                .create();
        filterNullGson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .setDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
                .create();
    }

    protected GsonUtils() {
    }

    /**
     * 根据对象返回json   不过滤空值字段
     */
    public static String toJsonWtihNullField(Object obj){
        return nullableGson.toJson(obj);
    }

    /**
     * 根据对象返回json  过滤空值字段
     */
    public static String toJsonFilterNullField(Object obj){
        return filterNullGson.toJson(obj);
    }

    /**
     * 将json转化为对应的实体对象
     * new TypeToken<HashMap<String, Object>>(){}.getType()
     */
    public static <T>  T fromJson(String json, Type type){
        return nullableGson.fromJson(json, type);
    }

    public static ActionFlag getJsonMsg(String json) throws Throwable {
        JSONObject jsonObject = new JSONObject(json);
        String act = jsonObject.getString("act");
        String iid= jsonObject.getString("iid");
        String payload = jsonObject.getString("payload");
        JSONArray jsonObject1 = new JSONObject(payload).getJSONArray("devices");
        JSONObject jsonObject2 = jsonObject1.getJSONObject(0);
        try {
            String name=jsonObject2.getString(act);
            return new ActionFlag(act,iid,name);
        }catch (Throwable e){
            return new ActionFlag(act,iid,"");
        }
    }
}
