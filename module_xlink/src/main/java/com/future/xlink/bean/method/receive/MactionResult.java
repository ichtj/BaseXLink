package com.future.xlink.bean.method.receive;

import com.future.xlink.bean.method.request.Mstruct;

import java.util.Map;

public class MactionResult extends Mstruct {
    public String _status;
    public String _description;
    public Map<String,Object> out;

    public MactionResult(String did, String method, String _status, String _description, Map<String, Object> out) {
        super(did, method);
        this._status = _status;
        this._description = _description;
        this.out = out;
    }

    public MactionResult(String _status, String _description, Map<String, Object> out) {
        this._status = _status;
        this._description = _description;
        this.out = out;
    }

    public MactionResult(String did, String method) {
        super(did, method);
    }

    public MactionResult() {
    }
}
