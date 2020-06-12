package com.tale.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import jetbrick.util.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.net.ssl.SSLContext;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * 普通的http访问，ssl的https访问，根据传进来的不同url进行自动选择。
 * 代理访问，传入代理就用代理访问，不传入就不用代理
 * get,post,put,delete
 * 2019.8.12
 * @author 钱志磊
 */
@Slf4j
public class HttpClientUtil {

    /**
     * 获取一个网络图片
     * @param url          获取图片的url
     * @return
     * @throws IOException
     */
    public static BufferedImage doGetImage(String url) throws IOException {
        log.info("get request url: {}", url);
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        HttpGet httpGet;
        try {
            httpclient = HttpClients.createDefault();
            // 创建http GET请求
            httpGet = new HttpGet(url);
            // 执行请求
            response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            BufferedImage image = ImageIO.read(entity.getContent());
            BufferedImage newImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            newImg.getGraphics().drawImage(image, 0, 0, null);
            return newImg;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }finally {
            if (httpclient != null) {
                httpclient.close();
            }
            if (response != null) {
                response.close();
            }
        }
    }

    /***下面是get请求**/

    public static Map<String, Object> doGet(String url) throws IOException {
        return doGet(url, null);
    }

    public static Map<String, Object> doGet(String url, JSONObject param) throws IOException {
        return doGet(url, param, "UTF8");
    }

    public static Map<String, Object> doGet(String url, JSONObject param, String charset) throws IOException {
        Map<String, String> header;
        header = getDefaultHeader();
        return doGet(url, header, param, 60 * 1000, 60 * 1000, charset);
    }

    public static Map doGet(String url, Map<String, String> header, JSONObject param, int connectionTimeOut, int readTimeOut, String charSet) throws IOException {
        return doGet(url, header, param, connectionTimeOut, readTimeOut, charSet, null, null, null, null, null);
    }

    /**
     * get请求
     * param  详情见post请求
     * @return
     * @throws IOException
     */
    public static Map<String, Object> doGet(String url, Map<String,String>header, JSONObject param, Integer connectTimeOut, Integer readTimeOut, String charset,String hostName, Integer port, String scheme, String username, String pass) throws IOException {
        log.info("get request url: {}, params: {}", url, JSON.toJSONString(param));
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        CookieStore cookieStore = new BasicCookieStore();
        HttpContext localContext = getLocalContext(cookieStore);
        HttpGet httpGet;
        try {
            httpclient = getHttpClient(url,hostName, port, scheme, username, pass);
            URI uri = getUri(param, url,charset);
            // 创建http GET请求
            httpGet = new HttpGet(uri);
            httpGet = init(httpGet, header);
            httpGet.setConfig(getConfig(connectTimeOut, readTimeOut));
            // 执行请求
            response = httpclient.execute(httpGet,localContext);
            return getResult(response,charset,cookieStore);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return getResult(e,charset,cookieStore);
        }finally {
            if (httpclient != null) {
                httpclient.close();
            }
            if (response != null) {
                response.close();
            }
        }
    }

    /***下面是post请求**/

    public static Map doPost(String url) throws IOException {
        return doPost(url, 60 * 1000, 60 * 1000);
    }

    public static Map doPost(String url, int connectionTimeOut, int readTimeout) throws IOException {
        return doPost(url, null, connectionTimeOut, readTimeout, "UTF-8", false);
    }

    public static Map doPost(String url, JSONObject param, int connectionTimeOut, int readTimeout, String charSet,boolean isHeader) throws IOException {
        Map<String, String> header = getDefaultHeader();
        return doPost(url, header, param, connectionTimeOut, readTimeout, charSet, isHeader);
    }

    public static Map doPost(String url, Map<String, String> header, JSONObject param, int connectionTimeOut, int readTimeOut, String charSet,boolean isHeader) throws IOException {
        return doPost(url, header, param, connectionTimeOut, readTimeOut, charSet, isHeader, null, null, null, null, null);
    }


    /**
     * @param url                   请求路径
     * @param header                请求头
     * @param param                 请求体
     * @param connectionTimeOut     链接超时时间
     * @param readTimeOut           读取超时时间
     * @param charSet               编码
     * @param isHeader              请求体是否链接在路径后面
     * @param hostName              代理地址
     * @param port                  代理端口
     * @param scheme                拦截器
     * @param username              代理用户名
     * @param pass                  代理密码
     * @return
     * @throws IOException
     */
    public static Map doPost(String url, Map<String, String> header, JSONObject param, int connectionTimeOut, int readTimeOut, String charSet,boolean isHeader,String hostName, Integer port, String scheme, String username, String pass) throws IOException {
        log.info("get request url: {}, params: {}", url, JSON.toJSONString(param));
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        CookieStore cookieStore = new BasicCookieStore();
        HttpContext localContext = getLocalContext(cookieStore);
        HttpPost post;
        try {
            httpclient = getHttpClient(url,hostName, port, scheme, username, pass);
            URI uri = getUri(isHeader ? param : null, url, charSet);
            // 创建http GET请求
            post = new HttpPost(uri);
            post = init(post, header);
            if (!isHeader && param != null && param.size() > 0) {
                ContentType contentType = getContentType(charSet, header.get("Content-Type"));
                post.setEntity(new StringEntity(param.toJSONString(), contentType));
            }
            post.setConfig(getConfig(connectionTimeOut,readTimeOut));
            // 执行请求
            response = httpclient.execute(post, localContext);
            return getResult(response, charSet,cookieStore);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return getResult(e,charSet,cookieStore);
        }finally {
            if (httpclient != null) {
                httpclient.close();
            }
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * 提取的公用方法
     * 获取一个http的上下文对象，用来存储更新过的cookie
     * @param cookieStore    cookie对象
     * @return
     */
    private static HttpContext getLocalContext(CookieStore cookieStore) {
        try {
            HttpContext localContext = new BasicHttpContext();
            localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
            return localContext;
        } catch (Exception e) {
            log.error(JSON.toJSONString(e.getStackTrace()));
            return null;
        }
    }

    /***下面是put请求**/

    public static Map doPut(String url) throws IOException {
        return doPut(url, 60 * 1000, 60 * 1000);
    }

    public static Map doPut(String url, int connectionTimeOut, int readTimeout) throws IOException {
        return doPut(url, null, connectionTimeOut, readTimeout, "UTF-8", false);
    }

    public static Map doPut(String url, JSONObject param, int connectionTimeOut, int readTimeout, String charSet,boolean isHeader) throws IOException {
        Map<String, String> header = getDefaultHeader();
        return doPut(url, header, param, connectionTimeOut, readTimeout, charSet, isHeader);
    }

    public static Map doPut(String url, Map<String, String> header, JSONObject param, int connectionTimeOut, int readTimeOut, String charSet,boolean isHeader) throws IOException {
        return doPut(url, header, param, connectionTimeOut, readTimeOut, charSet, isHeader, null, null, null, null, null);
    }

    /**
     * put请求
     * param 详情见post请求
     * @return
     * @throws IOException
     */
    public static Map doPut(String url, Map<String, String> header, JSONObject param, int connectionTimeOut, int readTimeOut, String charSet,boolean isHeader,String hostName, Integer port, String scheme, String username, String pass) throws IOException {
        log.info("get request url: {}, params: {}", url, JSON.toJSONString(param));
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        CookieStore cookieStore = new BasicCookieStore();
        HttpContext localContext = getLocalContext(cookieStore);
        HttpPut put;
        try {
            httpclient = getHttpClient(url,hostName, port, scheme, username, pass);
            URI uri = getUri(isHeader ? param : null, url, charSet);
            // 创建http GET请求
            put = new HttpPut(uri);
            put = init(put, header);
            put.setConfig(getConfig(connectionTimeOut,readTimeOut));
            if (!isHeader && param != null && param.size() > 0) {
                ContentType contentType = getContentType(charSet, header.get("Content-Type"));
                put.setEntity(new StringEntity(param.toJSONString(), contentType));
            }
            // 执行请求
            response = httpclient.execute(put,localContext);
            return getResult(response,charSet,cookieStore);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return getResult(e,charSet,cookieStore);
        }finally {
            if (httpclient != null) {
                httpclient.close();
            }
            if (response != null) {
                response.close();
            }
        }
    }

    /***下面是delete请求**/

    public static Map doDelete(String url) throws IOException {
        return doDelete(url, 60 * 1000, 60 * 1000);
    }

    public static Map doDelete(String url, int connectionTimeOut, int readTimeout) throws IOException {
        return doDelete(url, null, connectionTimeOut, readTimeout, "UTF-8");
    }

    public static Map doDelete(String url, JSONObject param, int connectionTimeOut, int readTimeout, String charSet) throws IOException {
        Map<String, String> header = getDefaultHeader();
        return doDelete(url, header, param, connectionTimeOut, readTimeout, charSet);
    }

    public static Map doDelete(String url, Map<String, String> header, JSONObject param, int connectionTimeOut, int readTimeOut, String charSet) throws IOException {
        return doDelete(url, header, param, connectionTimeOut, readTimeOut, charSet, null, null, null, null, null);
    }

    /**
     * delete请求
     * param 详情见post请求的解释
     * @return
     * @throws IOException
     */
    public static Map doDelete(String url, Map<String, String> header, JSONObject param, int connectionTimeOut, int readTimeOut, String charSet,String hostName, Integer port, String scheme, String username, String pass) throws IOException {
        log.info("get request url: {}, params: {}", url, JSON.toJSONString(param));
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        CookieStore cookieStore = new BasicCookieStore();
        HttpContext localContext = getLocalContext(cookieStore);
        HttpDelete delete;
        try {
            httpclient = getHttpClient(url,hostName, port, scheme, username, pass);
            URI uri = getUri(param, url, charSet);
            // 创建http GET请求
            delete = new HttpDelete(uri);
            delete = init(delete, header);
            delete.setConfig(getConfig(connectionTimeOut,readTimeOut));
            // 执行请求
            response = httpclient.execute(delete,localContext);
            return getResult(response,charSet,cookieStore);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return getResult(e, charSet, cookieStore);
        }finally {
            if (httpclient != null) {
                httpclient.close();
            }
            if (response != null) {
                response.close();
            }
        }
    }

    private final static String UTF8 = "UTF-8";

    /**
     * 初始化contentType
     * @param charSet       编码
     * @param mimeType      请求格式
     * @return
     */
    private static ContentType getContentType(String charSet, String mimeType) {
        if (StringUtils.isNotBlank(mimeType)) {
            mimeType = mimeType.split(";")[0];
        }else{
            mimeType = ContentType.APPLICATION_JSON.getMimeType();
        }
        if (StringUtils.isBlank(charSet)) {
            if (ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(mimeType)) {
                return ContentType.create(ContentType.APPLICATION_FORM_URLENCODED.getMimeType(), ContentType.APPLICATION_FORM_URLENCODED.getCharset());
            } else if (ContentType.APPLICATION_JSON.getMimeType().equals(mimeType)) {
                return ContentType.create(ContentType.APPLICATION_JSON.getMimeType(), ContentType.APPLICATION_JSON.getCharset());
            } else if (ContentType.APPLICATION_ATOM_XML.getMimeType().equals(mimeType)) {
                return ContentType.create(ContentType.APPLICATION_ATOM_XML.getMimeType(), ContentType.APPLICATION_ATOM_XML.getCharset());
            } else if (ContentType.APPLICATION_OCTET_STREAM.getMimeType().equals(mimeType)) {
                return ContentType.create(ContentType.APPLICATION_OCTET_STREAM.getMimeType(), ContentType.APPLICATION_OCTET_STREAM.getCharset());
            } else if (ContentType.APPLICATION_SVG_XML.getMimeType().equals(mimeType)) {
                return ContentType.create(ContentType.APPLICATION_SVG_XML.getMimeType(), ContentType.APPLICATION_SVG_XML.getCharset());
            } else if (ContentType.APPLICATION_XHTML_XML.getMimeType().equals(mimeType)) {
                return ContentType.create(ContentType.APPLICATION_XHTML_XML.getMimeType(), ContentType.APPLICATION_XHTML_XML.getCharset());
            } else if (ContentType.APPLICATION_XML.getMimeType().equals(mimeType)) {
                return ContentType.create(ContentType.APPLICATION_XML.getMimeType(), ContentType.APPLICATION_XML.getCharset());
            } else {
                charSet = UTF8;
            }
        }
        return ContentType.create(mimeType, charSet);
    }

    /**
     * 初始化HTTP的头部
     * @param abstractHttpMessage
     * @param header
     * @param <T>
     * @return
     */
    public static <T> T init(AbstractHttpMessage abstractHttpMessage,Map<String, String> header) {
        if (header != null) {
            for (String key : header.keySet()) {
                abstractHttpMessage.addHeader(key,header.get(key));
            }
        }
        return (T) abstractHttpMessage;
    }

    /**
     * 兼容代理的httpclient
     * @param url               请求路径
     * @param hostName          代理地址
     * @param port              代理端口
     * @param scheme            拦截器
     * @param username          代理用户名
     * @param pass              代理密码
     * @return
     */
    public static CloseableHttpClient getHttpClient(String url,String hostName, Integer port, String scheme, String username, String pass) {
        HttpHost proxy=null;
        CredentialsProvider provider=null;
        if (StringUtils.isNotBlank(hostName)) {
            if (port == null) {
                port = 80;
            }
            try {
                if (StringUtils.isBlank(scheme)) {
                    proxy = new HttpHost(hostName, port);
                } else {
                    proxy = new HttpHost(hostName, port, scheme);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            if (StringUtils.isNotBlank(username)) {
                if (StringUtils.isBlank(pass)) {
                    pass = "";
                }
                provider = new BasicCredentialsProvider();
                provider.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(username, pass));
            }
        }
        try {
            if (StringUtils.isNotBlank(url)) {
                if (url.contains("https")) {
                    SSLContext context = SSLContext.getDefault();
                    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(context);
                    if (provider != null) {
                        return HttpClients.custom().setProxy(proxy).setDefaultCredentialsProvider(provider).setSSLSocketFactory(sslsf).build();
                    } else if (proxy != null) {
                        return HttpClients.custom().setProxy(proxy).setSSLSocketFactory(sslsf).build();
                    }else{
                        return HttpClients.custom().setSSLSocketFactory(sslsf).build();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        if (provider != null) {
            return HttpClients.custom().setProxy(proxy).setDefaultCredentialsProvider(provider).build();
        } else if (proxy != null) {
            return HttpClients.custom().setProxy(proxy).build();
        }else{
            return HttpClients.createDefault();
        }
    }

    /**
     * 获取默认的头部
     * @return
     */
    public static Map getDefaultHeader() {
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        return header;
    }

    /**
     * 整理返回结果
     * @param object
     * @param charSet
     * @return
     * @throws IOException
     */
    public static Map getResult(Object object,String charSet,CookieStore cookieStore) throws IOException {
        Map<String, Object> result = new HashMap<>();
        if (object instanceof CloseableHttpResponse) {
            CloseableHttpResponse response = (CloseableHttpResponse)object;
            result.put("status", response.getStatusLine().getStatusCode());
            result.put("result", EntityUtils.toString(response.getEntity(), StringUtils.isNotBlank(charSet) ? charSet : UTF8));
            result.put("cookie", cookieStore.getCookies());
        }else{
            Exception response = (Exception)object;
            result.put("status", 500);
            result.put("result", response.getMessage());
            result.put("cookie", cookieStore.getCookies());
        }
        return result;
    }

    /**
     * 获取默认的配置信息
     * @param connectionTimeOut
     * @param readTimeOut
     * @return
     */
    public static RequestConfig getConfig(int connectionTimeOut, int readTimeOut) {
        return RequestConfig.custom().setConnectTimeout(connectionTimeOut).setSocketTimeout(readTimeOut).build();
    }

    /**
     * 获得URI对象
     * @param param
     * @param url
     * @return
     * @throws URISyntaxException
     */
    public static URI getUri(JSONObject param, String url,String charSet) throws URISyntaxException {
        URIBuilder builder;
        // 创建uri
        builder = new URIBuilder(url);
        Charset charset = Charset.forName(StringUtils.isNotBlank(charSet) ? charSet : UTF8);
        builder.setCharset(charset);
        if (param != null) {
            for (String key : param.keySet()) {
                builder.addParameter(key, param.getString(key));
            }
        }
        URI uri = builder.build();
        return uri;
    }


    /**
     * 发送钉钉预警 通过url
     * @param content
     * @param url
     */
    public static void sendDingdingMsgUrl(String url,String content) {
        sendDingdingMsgUrl(url, content, null);
    }

    /**
     * 发送钉钉预警  并@ 指定人员
     * @param content
     * @param url
     * @param earlyWarningPhones   手机号，中间以 "|" 分隔
     */
    public static void sendDingdingMsgUrl(String url,String content, String earlyWarningPhones) {
        //        content = URLEncoder.encode(content);
        //"{ \"msgtype\": \"text\", \"text\": {\"content\": \"" + content + "\"}}";
        JSONObject JSONObject_textMsg = new JSONObject();
        JSONObject_textMsg.put("msgtype", "text");
        JSONObject JSONObject_text = new JSONObject();
        JSONObject_text.put("content", content);
        JSONObject_textMsg.put("text", JSONObject_text);
        if (StringUtils.isNotBlank(earlyWarningPhones)) {
            String[] atSplit = earlyWarningPhones.split("[|]");
            JSONObject atJson = new JSONObject();
            JSONArray atMobiles = new JSONArray();
            //@的人手机号
            for (String string : atSplit) {
                atMobiles.add(string);
            }
            //将多个手机号塞入请求json
            atJson.put("atMobiles", atMobiles);
            JSONObject_textMsg.put("at", atJson);
        }
        try {
            doPost(url, JSONObject_textMsg, 60 * 1000, 60 * 1000, "UTF-8", false);
        }
        catch (Exception e) {
        }
    }

    /**
     * 发送加签
     * @param url
     * @param sign  秘钥
     * @param content
     * @param earlyWarningPhones
     */
    public static void sendDingBySign(String url, String sign, String content, String earlyWarningPhones) {
        url = url + "&" + getSignStr(sign);
        sendDingdingMsgUrl(url, content, earlyWarningPhones);
    }

    public static void sendDingBySign(String url, String sign, String content) {
        sendDingBySign(url, sign, content, null);
    }

    /**
     * 测试关键字
     * @param url
     * @param keyword  关键字
     * @param content
     * @param earlyWarningPhones
     */
    public static void sendDingByKeyword(String url, String keyword, String content, String earlyWarningPhones) {
        content = "【" + keyword + "】\n" + content;
        sendDingdingMsgUrl(url, content, earlyWarningPhones);
    }

    public static void sendDingByKeyword(String url, String keyword, String content) {
        content = "【" + keyword + "】\n" + content;
        sendDingdingMsgUrl(url, content, null);
    }

    /**
     * 获取签名字符串
     * @param sign  秘钥
     * @return
     */
    private static String getSignStr(String sign) {
        try {
            Long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + sign;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sign.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            String signStr = URLEncoder.encode(new String(Base64.encodeBase64(signData)),"UTF-8");
            return "timestamp=" + timestamp + "&sign=" + signStr;
        } catch (Exception e) {
            return "timestamp=&sign=";
        }
    }

}
