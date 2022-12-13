package com.future.xlink.callback;

import android.content.Context;

import com.future.xlink.bean.InitParams;

public abstract interface IConnect {

    void toConnect(InitParams initParams, Context context);
}
