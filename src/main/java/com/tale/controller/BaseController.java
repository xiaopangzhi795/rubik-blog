package com.tale.controller;

import com.alibaba.fastjson.JSONObject;
import com.blade.mvc.http.Request;
import com.tale.model.entity.Users;
import com.tale.utils.MapCache;
import com.tale.utils.TaleUtils;
import jetbrick.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

    public String getIpAddr(Request request) throws UnknownHostException {
        String Xip = request.header("X-Real-ip");
        String XFor = request.header("X-Forwarded-For");
        if (StringUtils.isNotBlank(Xip) && !"unKnown".equalsIgnoreCase(XFor)) {
            //多次反向代理后会有多个ip值，第一个ip才是真实ip
            int index = XFor.indexOf(",");
            if (index != -1) {
                return XFor.substring(0, index);
            } else {
                return XFor;
            }
        }
        XFor = Xip;
        if (StringUtils.isNotBlank(XFor)&& !"unKnown".equalsIgnoreCase(XFor)) {
            return XFor;
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.header("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.header("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.header("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.header("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = InetAddress.getLocalHost().getHostAddress();

        }
        return XFor;
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
