package com.tale.controller;

import com.alibaba.fastjson.JSONObject;
import com.blade.exception.ValidatorException;
import com.blade.ioc.annotation.Inject;
import com.blade.kit.StringKit;
import com.blade.mvc.annotation.*;
import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;
import com.blade.mvc.ui.RestResponse;
import com.tale.bootstrap.TaleConst;
import com.tale.extension.Commons;
import com.tale.model.dto.ErrorCode;
import com.tale.model.dto.Types;
import com.tale.model.entity.Comments;
import com.tale.model.entity.Contents;
import com.tale.service.CommentsService;
import com.tale.service.ContentsService;
import com.tale.service.SiteService;
import com.tale.utils.HttpClientUtil;
import com.tale.validators.CommonValidator;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Map;

import static com.tale.bootstrap.TaleConst.COMMENT_APPROVED;
import static com.tale.bootstrap.TaleConst.COMMENT_NO_AUDIT;
import static com.tale.bootstrap.TaleConst.OPTION_ALLOW_COMMENT_AUDIT;

/**
 * @author biezhi
 * @date 2018/6/4
 */
@Path
@Slf4j
public class ArticleController extends BaseController {

    @Inject
    private ContentsService contentsService;

    @Inject
    private CommentsService commentsService;

    @Inject
    private SiteService siteService;

    @GetRoute(value = {"/403"})
    public String err403(Request request) {
        return this.render_403();
    }

    @GetRoute(value = {"/userInfo"})
    @JSON
    public RestResponse<String> getInfo(Request request){
        UserAgent userAgent = UserAgent.parseUserAgentString(request.header("User-Agent")); //req就是request请求
        Browser browser = userAgent.getBrowser();   //获取浏览器信息 
        OperatingSystem os = userAgent.getOperatingSystem(); //获取操作系统信息
        StringBuffer userInfo = new StringBuffer();
        userInfo.append("操作系统：" + os.toString() + " \r\n 浏览器：" + browser.toString()).append(" \r\n HOST:" + request.header("Host"));
        String ip = "";
        try {
            ip = getIpAddr(request); //ip 地址
            String url = "https://www.devtool.top/api/ip/" + ip;
            Map<String, Object> response = HttpClientUtil.doGet(url);
            JSONObject result = JSONObject.parseObject(response.get("result").toString().trim());
            if (result.getInteger("code") == 200) {
                JSONObject data = result.getJSONObject("data");
                userInfo.append("\r\n IP:").append(data.getString("ip")).append("   ").append("地区:")
                        .append(data.getString("province")).append("  ").append(data.getString("city"));
                if (!data.getString("city").contains("内网")) {
                    url = "https://www.devtool.top/api/weather?city=" + data.getString("city").replaceAll("省", "").replaceAll("市", "");
                    response = HttpClientUtil.doGet(url);
                    result = JSONObject.parseObject(response.get("result").toString().trim());
                    if (result.getInteger("code") == 200) {
                        data = result.getJSONObject("data");
                        userInfo.append("\r\n").append("湿度：").append(data.getString("humi"))
                                .append("  天气：").append(data.getString("info"))
                                .append("  温度：").append(data.getString("quality")).append("~").append(data.getString("temp"))
                                .append("  ").append(data.getString("wind"));
                    }
                }
            }else{
                userInfo.append("IP:").append(ip).append("  地区:").append("未知");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            userInfo.append("IP： 未知");
        } catch (IOException e) {
            e.printStackTrace();
            userInfo.append("ip地址：" + ip);
        }
        return RestResponse.ok(userInfo.toString());
    }



    /**
     * 自定义页面
     */
    @GetRoute(value = {"/:cid", "/:cid.html"})
    public String page(@PathParam String cid, Request request) {
        Contents contents = contentsService.getContents(cid);
        if (null == contents) {
            return this.render_404();
        }
        if (contents.getAllowComment()) {
            int cp = request.queryInt("cp", 1);
            request.attribute("cp", cp);
        }
        request.attribute("article", contents);
        Contents temp = new Contents();
        temp.setHits(contents.getHits() + 1);
        temp.updateById(contents.getCid());
        if (Types.ARTICLE.equals(contents.getType())) {
            return this.render("post");
        }
        if (Types.PAGE.equals(contents.getType())) {
            return this.render("page");
        }
        return this.render_404();
    }

    /**
     * 文章页
     */
    @GetRoute(value = {"article/:cid", "article/:cid.html"})
    public String post(Request request, @PathParam String cid) {
        Contents contents = contentsService.getContents(cid);
        if (null == contents) {
            return this.render_404();
        }
        if (Types.DRAFT.equals(contents.getStatus())) {
            return this.render_404();
        }
        request.attribute("article", contents);
        request.attribute("is_post", true);
        if (contents.getAllowComment()) {
            int cp = request.queryInt("cp", 1);
            request.attribute("cp", cp);
        }
        Contents temp = new Contents();
        temp.setHits(contents.getHits() + 1);
        temp.updateById(contents.getCid());
        return this.render("post");
    }

    /**
     * 评论操作
     */
    @PostRoute(value = "comment")
    @JSON
    public RestResponse<?> comment(Request request, Response response,
                                   @HeaderParam String Referer, Comments comments) {
        try {
            if (StringKit.isBlank(Referer)) {
                return RestResponse.fail(ErrorCode.BAD_REQUEST);
            }

            if (!Referer.startsWith(Commons.site_url())) {
                return RestResponse.fail("非法评论来源");
            }

                CommonValidator.valid(comments);


            String  val   = request.address() + ":" + comments.getCid();
            Integer count = cache.hget(Types.COMMENTS_FREQUENCY, val);
            if (null != count && count > 0) {
                return RestResponse.fail("您发表评论太快了，请过会再试");
            }
            comments.setIp(request.address());
            comments.setAgent(request.userAgent());

            if (TaleConst.OPTIONS.getBoolean(OPTION_ALLOW_COMMENT_AUDIT, true)) {
                comments.setStatus(COMMENT_NO_AUDIT);
            } else {
                comments.setStatus(COMMENT_APPROVED);
            }

            try {
                commentsService.saveComment(comments);
                response.cookie("tale_remember_author", URLEncoder.encode(comments.getAuthor(), "UTF-8"), 7 * 24 * 60 * 60);
                response.cookie("tale_remember_mail", URLEncoder.encode(comments.getMail(), "UTF-8"), 7 * 24 * 60 * 60);
                if (StringKit.isNotBlank(comments.getUrl())) {
                    response.cookie("tale_remember_url", URLEncoder.encode(comments.getUrl(), "UTF-8"), 7 * 24 * 60 * 60);
                }

                // 设置对每个文章30秒可以评论一次
                cache.hset(Types.COMMENTS_FREQUENCY, val, 1, 30);
                siteService.cleanCache(Types.SYS_STATISTICS);

                return RestResponse.ok();
            } catch (Exception e) {
                String msg = "评论发布失败";
                if (e instanceof ValidatorException) {
                    msg = e.getMessage();
                } else {
                    log.error(msg, e);
                }
                return RestResponse.fail(msg);
            }
        } catch (Exception e) {
            return RestResponse.fail(e.getMessage());
        }
    }

}
