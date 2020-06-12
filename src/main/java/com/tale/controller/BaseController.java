package com.tale.controller;

import com.alibaba.fastjson.JSONObject;
import com.blade.mvc.http.Request;
import com.tale.model.entity.Users;
import com.tale.utils.MapCache;
import com.tale.utils.TaleUtils;

/**
 * Created by biezhi on 2017/2/21.
 */
public abstract class BaseController {

    public static String THEME = "themes/default";

    protected MapCache cache = MapCache.single();

    public String render(String viewName) {
        return THEME + "/" + viewName;
    }

    public BaseController title(Request request, String title) {
        request.attribute("title", title);
        return this;
    }

    public BaseController keywords(Request request, String keywords) {
        request.attribute("keywords", keywords);
        return this;
    }

    protected static JSONObject beanToJSONObject(Object obj) {
        return JSONObject.parseObject(JSONObject.toJSONString(obj));

    }

    public Users user() {
        return TaleUtils.getLoginUser();
    }

    public Integer getUid(){
        return this.user().getUid();
    }

    public String render_404() {
        return "/comm/error2_404.html";
    }

    public String render_403() {
        return "/comm/error_403";
    }

}
