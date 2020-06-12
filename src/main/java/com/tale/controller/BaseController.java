package com.tale.controller;

import com.alibaba.fastjson.JSONObject;
import com.blade.mvc.http.Request;
import com.tale.model.entity.Users;
import com.tale.utils.MapCache;
import com.tale.utils.TaleUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by biezhi on 2017/2/21.
 */
@Slf4j
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

    public String getIpAddr(Request request) {
        log.info(JSONObject.toJSONString(request));
        String ip = request.header("x-forwarded-for");
        if(ip == null || ip.length() == 0 ||"unknown".equalsIgnoreCase(ip)) {
            ip = request.header("Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 ||"unknown".equalsIgnoreCase(ip)) {
            ip = request.header("WL-Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 ||"unknown".equalsIgnoreCase(ip)) {
            ip = request.remoteAddress();
        }
        log.info(request.header("x-forwarded-for"));
        log.info(request.header("Proxy-Client-IP"));
        log.info(request.header("WL-Proxy-Client-IP"));
        log.info(request.header("X-Real-IP"));
        return ip;
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
