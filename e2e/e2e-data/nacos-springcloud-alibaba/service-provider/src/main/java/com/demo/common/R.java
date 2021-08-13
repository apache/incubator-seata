package com.demo.common;


import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class R {
    private boolean success;
    private Map<String, Object> data = new HashMap<String, Object>();

    private R() {
    }

    public static R ok() {
        R r = new R();
        r.setSuccess(true);
        return r;
    }

    public static R error() {
        R r = new R();
        r.setSuccess(false);
        return r;
    }

    public R data(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public R data(Map<String, Object> map) {
        this.setData(map);
        return this;
    }
}
