package com.future.xlink.callback;

import androidx.annotation.IntDef;

import com.future.xlink.bean.PutType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({PutType.EVENT,PutType.METHOD,PutType.SETPERTIES,PutType.GETPERTIES,PutType.UPLOAD})
@Retention(RetentionPolicy.SOURCE)
public @interface IPutType {
}
