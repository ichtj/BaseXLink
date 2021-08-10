package com.future.xlink.utils;

import android.content.Context;
import android.util.Log;

import com.future.xlink.bean.Constants;
import com.future.xlink.bean.Register;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class PropertiesUtil {
	private static final String TAG = "PropertiesUtil";
	private Context mContext;
	private String mPath;
	private String mFile;
	private Properties mProp;
	private static PropertiesUtil mPropUtil = null;

	public static PropertiesUtil getInstance(Context context) {
		if (mPropUtil == null) {
			mPropUtil = new PropertiesUtil();
			mPropUtil.mContext = context;
			mPropUtil.mPath=GlobalConfig.PROPERT_URL;
			mPropUtil.mFile = GlobalConfig.MY_PROPERTIES;
		}
		return mPropUtil;
	}

	public PropertiesUtil setPath(String path) {
		mPath = path;
		return this;
	}

	public PropertiesUtil setFile(String file) {
		mFile = file;
		return this;
	}

	public PropertiesUtil init() {
		try {
			File dir = new File(mPath);
			File file = new File(dir, mFile);
			InputStream is = new FileInputStream(file);
			mProp = new Properties();
			mProp.load(is);
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "init: errMeg="+e.getMessage());
		}
		return this;
	}

	public void commit() {
		try {
			File file = new File(mPath +  mFile);
			OutputStream os = new FileOutputStream(file);
			mProp.store(os, "");
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mProp.clear();
	}

	public void clear() {
		mProp.clear();
	}

	public void open() {

		try {
			mProp.clear();
			File file = new File(mPath+mFile);
			InputStream is = new FileInputStream(file);
			mProp = new Properties();
			mProp.load(is);
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    public void writeString(String name, String value) {
    	mProp.setProperty(name, value);
    }

    public String readString(String name, String defaultValue) {
        return mProp.getProperty(name, defaultValue);
    }

    public void writeInt(String name, int value) {
    	mProp.setProperty(name, ""+value);
    }

    public int readInt(String name, int defaultValue) {
        return Integer.parseInt(mProp.getProperty(name, ""+defaultValue));
    }

    public void writeBoolean(String name, boolean value) {
    	mProp.setProperty(name, ""+value);
    }

    public boolean readBoolean(String name, boolean defaultValue) {
        return Boolean.parseBoolean(mProp.getProperty(name, ""+defaultValue));
    }

    public void writeDouble(String name, double value) {
    	mProp.setProperty(name, ""+value);
    }

    public double readDouble(String name, double defaultValue) {
        return Double.parseDouble(mProp.getProperty(name, ""+defaultValue));
    }
	public static void saveProperties(Context mContext,Register register) {
		PropertiesUtil mProp = PropertiesUtil.getInstance(mContext).init();
		//保存存储参数
		mProp.writeString(Constants.SSID, register.ssid);
		mProp.writeString(Constants.MQTTBROKER, register.mqttBroker);
		mProp.writeString(Constants.MQTTUSERNAME, register.mqttUsername);
		mProp.writeString(Constants.MQTTPASSWORD, register.mqttPassword);

		mProp.commit();
	}

	/**
	 * 清除保存的缓存数据
	 * */
	public  static  void  clearProperties(Context mContext){
		PropertiesUtil mProp = PropertiesUtil.getInstance(mContext).init();

		mProp.open();
		//保存存储参数
		mProp.writeString(Constants.SSID, "");
//		mProp.writeString(Constants.MQTTBROKER, "");
		mProp.writeString(Constants.MQTTUSERNAME, "");
		mProp.writeString(Constants.MQTTPASSWORD, "");
		mProp.commit();
	}
	public static Register getProperties(Context mContext) {
		PropertiesUtil mProp = PropertiesUtil.getInstance(mContext).init();
		mProp.open();
		Register register = new Register();
		register.mqttBroker = mProp.readString(Constants.MQTTBROKER, "");
		register.ssid = mProp.readString(Constants.SSID, "");
		register.mqttUsername = mProp.readString(Constants.MQTTUSERNAME, "");
		register.mqttPassword = mProp.readString(Constants.MQTTPASSWORD, "");
		return register;

	}
}