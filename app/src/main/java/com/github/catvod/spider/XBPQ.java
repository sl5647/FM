package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Misc;
import com.github.catvod.utils.okhttp.OKCallBack;
import com.github.catvod.utils.okhttp.OkHttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.InetAddress;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Response;

import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;


public class XBPQ extends Spider {

    /**
     * 筛选
     */
    private boolean isFilter = false;
    private String CATEURL;
    private String cateData;

    /**
     * 首页
     */
    private boolean isHome = false;
    private int homeCount;
    private String homeUrl;

    /**
     * 图片代理
     */
    private boolean picProxy = false;

    /**
     * 直接播放
     */
    private boolean playDirect = false;
    private boolean shortVideo = false;
    private List<String> directPlayList = null;

    /**
     * 调试
     */
    private boolean debug = false;
    private String debugInfo;
    private String debugAt;
    private int debugStart = 0;

    protected JSONObject rule = null;

    /**
     * json模式
     */
    private boolean jsonMode = false;

    private String cookie="";

    /**
     * 当前分类
     */
    private String activeCate="";

    /**
     * 附加命令
     */
    private String oComand="";

    /**
     * 过盾
     */
    private boolean bt = false;

    private String jiequshuzuqianTemp="";

    private static String charSet;

    private boolean reverse = false;

    private PushAgent pushAgent;

    @Override
    public void init(Context context) {
        super.init(context);
    }

    @Override
    public void init(Context context, String extend) {
        super.init(context, extend);
            if (extend != null) {
                try {
                    if (extend.startsWith("http")) {
                        if (!extend.contains("{cateId}")) {
                            String json = OkHttpUtil.string(extend, null);
                            rule = new JSONObject(json);
                        } else {
                            rule = new JSONObject();
                            rule.put("分类url", extend);
                        }
                    } else if (extend.startsWith("{")) {
                        rule = new JSONObject(extend);
                    } else {
                            extend = extend.replace("s://", "s//").replace("p://", "p//").replace("包含:", "包含").replace("替换:", "替换").replace("序号:", "序号").replace("排序:", "排序").replace("script:", "script");
                            rule = new JSONObject();
                            if (!extend.contains(",")) {
                                rule.put(extend.split(":")[0], extend.split(":")[1].replace("s//", "s://").replace("p//", "p://").replace("包含", "包含:").replace("替换", "替换:").replace("序号", "序号:").replace("排序", "排序:").replace("script", "script:"));
                            } else {
                                for (String extW: extend.split(",")){
                                    rule.put(extW.split(":")[0], extW.split(":")[1].replace("s//", "s://").replace("p//", "p://").replace("包含", "包含:").replace("替换", "替换:").replace("序号", "序号:").replace("排序", "排序:").replace("script", "script:"));
                                }
                                
                            }
                     }
                } catch (JSONException e) {
                }
                homeUrl = getRuleVal("主页url");
                homeUrl = homeUrl.endsWith("/") ? homeUrl.substring(0, homeUrl.length()-1) : homeUrl;
                CATEURL = getRuleVal("分类url", "分类页", "class_url", "cateUrl", "");
                if (CATEURL.contains(";;")) {
                    if (CATEURL.split(";;").length>1)
                        oComand = CATEURL.split(";;")[1];
                    CATEURL = CATEURL.split(";;")[0];
                }
                getCookie();
                picProxy = getRuleVal("图片代理", "PicNeedProxy","0").equals("1") || oComand.contains("t");
                debugAt = oComand.contains("T") ? "1" : getRuleVal("调试", "debug", "");
                debug = (debugAt.length()>0 && !"0".equals(debugAt));
                if (debugAt.contains("$")) {
                    debugStart = Integer.parseInt(debugAt.split("\\$")[1]);
                    debugAt = debugAt.split("\\$")[0];
                }
                charSet =  oComand.contains("g") ? "GBK" : getRuleVal("编码");
                reverse = getRuleVal("倒序", "倒序播放", "epi_reverse", "0").equals("1") || (!oComand.contains("d0") && oComand.contains("d"));
                pushAgent = new PushAgent();
                pushAgent.init(context, getRuleVal("阿里token"));
            }
    }
/*
    public static void log(String msg) {
        try {
            PrintStream out = new PrintStream(new FileOutputStream("log.txt", true));
            System.setOut(out);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
            String strTime = sdf.format(new Date());
            System.out.println(strTime + ": " + msg);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
*/
    @Override
    public String homeContent(boolean filter) {
        try {
            JSONObject result = new JSONObject();
            JSONArray classes = new JSONArray();
            cateData = getCate();
            String cates = getRuleVal("列表分类", "fenlei", "");
            if (cates.isEmpty()) cates = cateData;
            for (String cate : cates.split("#")) {
                String[] info = cate.split("\\$");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type_name", info[0]);
                jsonObject.put("type_id", info[1]);
                classes.put(jsonObject);
            }
            result.put("class", classes);

            String filterName = "";
            if (rule.optJSONObject("筛选") != null || !getRuleVal("筛选").isEmpty()) {
                filterName = "筛选";
            } else if  (rule.optJSONObject("filter") != null) {
                filterName = "filter";
            } else if  (rule.optJSONObject("filterdata") != null) {
                filterName = "filterdata";
            }
            JSONObject filterJson = rule.optJSONObject(filterName);
            String strFilter = getRuleVal(filterName, "筛选数据", "");
            String cateUrl = CATEURL;
            isFilter = cateUrl.contains("{class}") || cateUrl.contains("{area}") || cateUrl.contains("{year}") || cateUrl.contains("{by}") || getRuleVal("类型", "筛选子分类名称", "").length()>1 || (filterJson!=null && filterJson.length()>0) || strFilter.length()>1;
            if (filter && isFilter) {
                if (strFilter.startsWith("http") || strFilter.startsWith("clan")) {
                    InetAddress localHost = InetAddress.getLocalHost();
                    String http = "http://" + localHost.getHostAddress() + ":9978/file/";
                    if (strFilter.startsWith("clan://")) {
                        if (strFilter.startsWith("clan://localhost/")) {
                            strFilter = strFilter.replace("clan://localhost/", http);
                        } else {
                            strFilter = strFilter.replace("clan://", http);
                        }
                    }

                    String json = OkHttpUtil.string(strFilter, null);
                    if (json != null)
                        filterJson = new JSONObject(json);
                } else if (filterJson == null || strFilter.equals("ext")) {
                    filterJson = getFilterData();
                }
                if (filterJson != null)
                result.put("filters", filterJson);
            }
            return result.toString();
        } catch (
                Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    protected HashMap<String, String> getHeaders(String url) {
        HashMap<String, String> headers = new HashMap<>();
        String user = getRuleVal("请求头", "头部集合", "ua", "UserAgent", "").trim(), ua ="";
        if (user.contains("手机") || user.contains("MOBILE_UA") || oComand.contains("a") || user.isEmpty() || user.contains("电脑") || user.contains("PC_UA")) {
            if (user.contains("手机") || user.contains("MOBILE_UA") || oComand.contains("a")) {
                ua = Misc.MoAgent;
            } else {
                ua = Misc.UaWinChrome;
            }
            headers.put("User-Agent", ua);
        }
        if(cookie.length()>0){
            headers.put("Cookie", cookie);
        }
        if (!user.contains("Referer")) {
            if (oComand.contains("r")) {
                headers.put("Referer", homeUrl + "/");
            }
        }
        user = user.replaceAll(".*电脑#","").replaceAll(".*手机#","");
        if (user.contains("$")) {
           for (String info: user.split("#")) {
               headers.put(info.split("\\$")[0], info.split("\\$")[1]);
           }
        }
        return headers;
    }

    protected String getPlayHeaders() {
        try {
            String head = getRuleVal("播放请求头", "play_header", "").trim(), ua = "";
            if (head.isEmpty() && !oComand.contains("A") && oComand.contains("W")) return "";
            JSONObject headers = new JSONObject();
            if (head.contains("手机") || head.contains("电脑") || oComand.contains("A") || oComand.contains("W")) {
                if (head.contains("手机") || oComand.contains("A")) {
                    ua = Misc.MoAgent;
                } else {
                    ua = Misc.UaWinChrome;
                }
                headers.put("User-Agent", ua);
            }
            if(cookie.length()>0){
                headers.put("Cookie", cookie);
            }
            if (!head.contains("Referer")) {
                if (oComand.contains("R")) {
                    headers.put("Referer", homeUrl + "/");
                }
            }
            head = head.replaceAll(".*电脑#","").replaceAll(".*手机#","");
            if (head.contains("$")) {
               for (String info: head.split("#")) {
                   headers.put(info.split("\\$")[0], info.split("\\$")[1]);
               }
            }
            return headers.toString();
        } catch (JSONException e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    protected void getCookie(){
        String cookieurl = homeUrl + "/";
        Map<String, List<String>> cookies = new HashMap<>();
        OkHttpUtil.string(cookieurl, getHeaders(cookieurl), cookies);
        for (Map.Entry<String, List<String>> entry : cookies.entrySet()) {
            if (entry.getKey().equals("set-cookie") || entry.getKey().equals("Set-Cookie")) {
                cookie = TextUtils.join(";", entry.getValue());
                break;
            }
        }
    }

    @Override
    public String homeVideoContent() {
        try {
            String homeCate = "";
            String temp = getRuleVal("首页", "热门", "homeContent", "shouye", "40");
            if (temp.equals("1") || temp.equals("首页")) temp ="40";
            String cateTemp = getRuleVal("列表分类", "fenlei", "").length()<3 ? cateData + "#" : getRuleVal("列表分类", "fenlei", "") + "#";
            String homePg = "";
            homeCount = 40;
                if (temp.contains("$")) {
                    homeCount = Integer.parseInt(temp.split("\\$")[1]);
                    homeCate = temp.split("\\$")[0];
                    homeCate = homeCate.equals("首页") ? "" : cateTemp.replaceAll(".*" + homeCate + "\\$(.*?)#.*", "$1");
                    homePg = getRuleVal("起始页", "qishiye", "firstpage", "1");
                } else {
                    if (temp.matches("\\d+")) {
                        homeCount = Integer.parseInt(temp);
                    } else {
                        homeCate = cateTemp.replaceAll(".*" + temp + "\\$(.*?)#.*", "$1");
                        homePg = getRuleVal("起始页", "qishiye", "firstpage", "1");
                    }
                }
            if (homeCount>0) {
                isHome = true;
                JSONObject result = category(homeCate, homePg, false, new HashMap<>());
                isHome = false;
                return result==null ? "" : result.toString();
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

//获取分类页网址
    protected String categoryUrl(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        String cateUrl = CATEURL;
        String qishiye = getRuleVal("起始页", "qishiye", "firstpage", "1");
        if (cateUrl.contains("[") || cateUrl.contains("|")) {
            if (pg.equals(qishiye)) {
                cateUrl = cateUrl.replaceAll(".*[\\[|\\|].*(http[^\\]]*)\\]?.*", "$1").replace("firstPage=", "");
            } else {
                cateUrl = cateUrl.replaceAll("\\|\\|", "\\|").replaceAll("(.*)[\\[|\\|].*", "$1");
            }
        }
        if (filter && isFilter && extend != null && extend.size() > 0) {
            for (Iterator<String> it = extend.keySet().iterator(); it.hasNext(); ) {
                String key = it.next();
                String value = extend.get(key);
                if (value.length() > 0) {
                    cateUrl = cateUrl.replace("{" + key + "}", URLEncoder.encode(value));
                }
            }
        }
        cateUrl = cateUrl.replace("{cateId}", tid).replace("{catePg}", pg);
        Matcher m = Pattern.compile("\\{(.*?)\\}").matcher(cateUrl);
        while (m.find()) {
            String n = m.group(0).replace("{", "").replace("}", "");
            cateUrl = cateUrl.replace(m.group(0), "").replace("/" + n + "/", "");
        }
        return cateUrl;
    }

    private JSONArray getJsonArray(String content, String jq) {
       try {
          if (jq.length()<1) return new JSONArray(content);
          if (jq.contains("&&")) jq = "data";
          JSONArray array = new JSONArray();
          String subscript = "";
          if (jq.contains("[")) {
              subscript = jq.replaceAll(".*\\[(.*?)\\].*", "$1");
              jq = jq.replaceAll("\\[.*", "");
          }
          String[] keylen = jq.split("\\.");
          String temp = content;
          for (int i=0; i<keylen.length; i++) {
             JSONObject data = new JSONObject(temp);
             if(i==keylen.length-1){
                 Object jObj = data.get(keylen[i]);
                 if (jObj instanceof JSONObject) {
                     array.put(data.getJSONObject(keylen[i]));
                     return array;
                 }
                 JSONArray arrayTemp = data.getJSONArray(keylen[i]);
                 int m=0, n=arrayTemp.length();
                 if (subscript!=null && subscript.length()>0) {
                    if (!subscript.contains(",") && subscript.matches("\\d+")) {
                        if (n>Integer.parseInt(subscript))
                           n = Integer.parseInt(subscript);
                        m = n-1;
                    } else {
                        String left = subscript.replaceAll("(.*),.*", "$1"), right = subscript.replaceAll(".*,(.*)", "$1");
                        if (right!=null && right.length()>0 && right.matches("\\d+") && Integer.parseInt(right)<n) n = Integer.parseInt(right);
                        if (left!=null && left.length()>0 && left.matches("\\d+") && Integer.parseInt(left)<=n) m = Integer.parseInt(left)-1;
                    }
                    for (; m<n; m++) {
                        array.put(arrayTemp.getJSONObject(m));
                    }
                    return array;
                }
                return arrayTemp;
            }
            temp = data.getJSONObject(keylen[i]).toString();
         }
         return null;
        } catch (JSONException e) {
            SpiderDebug.log(e);
        }
        return null;
    }

    private String getJsonArrayString(String content, String jq) {
        if (!jq.contains("+")) return getJsonArrayStringAction(content, jq);
        String[] strList = jq.split("\\+");
        String temp = "";
        for (int i=0; i<strList.length; i++) {
            String str = getJsonArrayStringAction(content, strList[i]);
            if (str.length()>0) temp += str;
        }
        return temp;
    }

    private String getJsonArrayStringAction(String content, String jq) {
       try {
            if (jq.indexOf("'")>=0) return jq.replace("'", "");
            if (jq.contains("&&") || jq.length()<1) jq = "data";
            if (!jq.contains("].")) return getJsonString(content, jq);
            String fenge = ",";
            String[] jqArray = jq.split("\\]\\.");
            String suf = "";
            if (jqArray.length>2) {
                for (int j=0; j<jqArray.length-2; j++) {
                    String jqTemp = jqArray[j] + "]";
                    content= getJsonArray(content, jqTemp).getJSONObject(0).toString();
                }
            }
            suf = jqArray[jqArray.length-1];
            jq = jqArray[jqArray.length-2] + "]";
            if (suf.contains("(")) {
                fenge = suf.replaceAll(".*\\((.*?)\\).*", "$1");
                suf = suf.replaceAll("\\(.*", "");
            }
            JSONArray array = getJsonArray(content, jq);
            String str = "";
            if (array!=null && array.length()>0) {
                for (int i=0; i<array.length(); i++) {
                    String data = array.getJSONObject(i).toString();
                    if (i==array.length()-1) fenge = "";
                    str = str + getJsonString(data, suf) + fenge;
                }
                return str;
            }
            return "";
        } catch (JSONException e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private String getJsonString(String content, String jq) {
       try {
            if (jq.contains("&&") || jq.length()<1) jq = "data";
            JSONArray array = null;
            String str = "";
            String strTemp = content;
            boolean isJson = false;
            if (jq.endsWith(";json;")) {
                isJson = true;
                jq = jq.substring(0, jq.length()-6);
            }
            if (!jq.contains("[")) {
                String[] keylen = jq.split("\\.");
                for (int i=0; i<keylen.length; i++) {
                    JSONObject data = new JSONObject(strTemp);
                    if(i==keylen.length-1){
                        if (isJson) {
                            Object jObj = data.get(keylen[i]);
                            if (jObj instanceof JSONObject) {
                               str = ((JSONObject)jObj).toString();
                            } else if (jObj instanceof JSONArray) {
                               JSONObject jsonObject = (new JSONObject()).put(keylen[i], ((JSONArray)jObj));
                               str = jsonObject.toString();
                            }
                        } else {
                            str = data.optString(keylen[i]).trim().replaceAll("\\]", "").replaceAll("\\[", "").replaceAll("\"", "");
                        }
                        if (str!=null) return str;
                        return "";
                    }
                    strTemp = data.getJSONObject(keylen[i]).toString();
                }
            }
            return "";
        } catch (JSONException e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private JSONObject category(String tid, String pg, boolean filter, HashMap<String, String> extend) {
       try {
            JSONObject result = getMovieList(tid, pg, filter, extend);
            JSONArray videos = result.getJSONArray("list");
            if ((videos.length()<1 || videos==null) && !"搜索".equals(activeCate) && jiequshuzuqianTemp.length()<1) {
                jiequshuzuqianTemp = "<a&&</a>";
                result = getMovieList(tid, pg, filter, extend);
            }
            return result;
        } catch (JSONException e) {
            SpiderDebug.log(e);
        }
       return null;
    }

    private JSONObject getMovieList(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        if (tid.length()<1) {
            activeCate = "首页";
        } else if (!"搜索".equals(activeCate)) {
            String cateTemp = "#" + cateData + "#";
            activeCate = cateTemp.replaceAll(".*#(.*?)\\$" + tid + "#.*", "$1");
        }
           String shortVideoStr = oComand.contains("D") ? "1" : getRuleVal("短视频");
           String playDirectStr = oComand.contains("z") ? "1" : getRuleVal("直接播放", "force_play", "");
           playDirect = false;
           shortVideo = false;
           if ("1".equals(shortVideoStr)) {
               shortVideo = true;
               playDirect = true;
           } else if (shortVideoStr.length()>1 && activeCate.length()>0) {
               for (String str: shortVideoStr.split("#")) {
                   if (activeCate.equals(str)) {
                       shortVideo = true;
                       playDirect = true;
                       break;
                   }
               }
           } else if ("1".equals(playDirectStr)) {
               playDirect = true;
           } else if (playDirectStr.length()>1 && activeCate.length()>0) {
               for (String str: playDirectStr.split("#")) {
                   if (activeCate.equals(str)) {
                       playDirect = true;
                       break;
                   }
               }
           }
        try {
            String webUrl="", key="";
            boolean quick = false;
            String qishiye = getRuleVal("起始页", "qishiye", "firstpage", "1");
            if ("搜索".equals(activeCate)) {
                if (tid.contains("##")) {
                    webUrl = tid.split("##")[0];
                    key = tid.split("##")[1];
                    if (key.contains("quick")) {
                        quick = true;
                        key = key.replace("quick", "");
                    }
                } else {
                    webUrl = tid;
                }
            } else if ("".equals(tid)) {
                webUrl = homeUrl;
            } else {
                if (tid.equals("空"))
                    tid = "";
                pg = String.valueOf(Integer.parseInt(pg) - 1 + Integer.parseInt(qishiye));
                if (getRuleVal("列表分类", "fenlei", "").isEmpty()) {
                    webUrl = categoryUrl(tid, pg, filter, extend);
                } else {
                    webUrl = homeUrl + tid + pg + getRuleVal("列表后缀", "houzhui", "");
                }
            }
            String html = webUrl.contains(";post") ? fetchPost(webUrl) : fetch(webUrl.split(";")[0]);

            if (key.length()>0) {
                if (html.contains("没有找到")) return null;
                html = html.replaceAll("class=\"pages\"[\\S\\s]+", "").replaceAll("热门电[\\S\\s]+", "").replaceAll("感兴趣[\\S\\s]+", "").replaceAll("热播影[\\S\\s]+", "");
            }

            String parseContent = html;
            JSONArray videos = new JSONArray();
            boolean shifouercijiequ = !getRuleVal("二次截取", "jiequqian", "cat_twice_pre", "").isEmpty();
            if (getRuleVal("列表二次截取").contains("$$")) shifouercijiequ = true;
            if (shifouercijiequ) {
                String jiequqian = getRuleVal("二次截取", "jiequqian", "cat_twice_pre", "");
                if (getRuleVal("列表二次截取").contains("$$")) jiequqian = getRuleVal("列表二次截取");
                String jiequhou = getRuleVal("jiequhou", "cat_twice_suf", "");
                parseContent = subContent(html, jiequqian, jiequhou).get(0);
                if (parseContent.isEmpty()) parseContent = html;
            }
            String jiequshuzuqian = getRuleVal("数组", "列表截取数组", "cateVodNode", "jiequshuzuqian", "catjsonlist", "cat_arr_pre", "");
            if (jiequshuzuqian.startsWith("//")) {
                String cateN = jiequshuzuqian;
                String cateName = getRuleVal("标题", "cateVodName", "/@title");
                String cateImg = getRuleVal("图片", "cateVodImg", "/@data-original");
                String cateId = getRuleVal("链接", "cateVodId", "/@href");
                String cateRmark = getRuleVal("副标题", "cateVodMark", "");

                JXDocument cateDoc = xpFetch(webUrl);
                 List<JXNode> urlNodes = cateDoc.selN(cateN);
                 List<String> vodItems = new ArrayList<>();
                 for (int k = 0; k < urlNodes.size(); k++) {
                    String title = urlNodes.get(k).selOne(cateName).asString().trim();
                    String link = urlNodes.get(k).selOne(cateId).asString().trim();
                    String pic = urlNodes.get(k).selOne(cateImg).asString().trim();
                    String remark = urlNodes.get(k).selOne(cateRmark).asString().trim();
                    if (link==null || link.length()<1 || (oComand.contains("k") && quick && key.length()>0 && !title.contains(key))) continue;
                    pic = Misc.fixUrl(webUrl, pic);
                    if(picProxy){
                       pic = fixCover(pic,webUrl);
                    }
                    if (debug) title = "xp:" + link;
                    JSONObject v = new JSONObject();
                    if (playDirect) {
                        if (shortVideo) {
                            v.put("vod_id", title + "$$$" + pic + "$$$" + link+ "$$$" + "shortVideo");
                            directPlayList.add(title + "$" + link);
                         } else {
                            v.put("vod_id", title + "$$$" + pic + "$$$" + link+ "$$$" + "playDirect");
                        }
                    } else {
                        v.put("vod_id", title + "$$$" + pic + "$$$" + link);
                    }
                    v.put("vod_name", title);
                    v.put("vod_pic", pic);
                    v.put("vod_remarks", remark);
                    videos.put(v);
                    if (isHome && videos.length()>=homeCount) {
                        break;
                    }
                 }
            }

            if (videos.length()<1 || !jiequshuzuqian.startsWith("//")) {
            String jiequshuzuhou = getRuleVal("jiequshuzuhou", "cat_arr_suf", "");
            String titleV = getRuleVal("标题", "列表标题", "biaotiqian", "catjsonname", "cat_title", "title=\"&&\""), linkV = getRuleVal("链接", "列表链接", "lianjieqian", "catjsonid", "cat_url", "href=\"&&\"[不包含:script#/hot/#.xml#.js#=http]"), picV = getRuleVal("图片", "列表图片", "tupianqian", "catjsonpic", "cat_pic", "original=\"&&\""), remarkV = getRuleVal("副标题", "列表副标题", "fubiaotiqian", "catjsonstitle", "cat_subtitle", "class=\"pic-text*>&&<");
            ArrayList<String> jiequContents = subContent(parseContent, jiequshuzuqian, jiequshuzuhou);
            if (jiequshuzuqian.length()<1) {
                if (jiequshuzuqianTemp.length()<1) {
                    String[] playArray = {"class=\"stui-vodlist__box&&</div>", "class=\"myui-vodlist__box&&</div>", "class=\"module-item-pic\"&&</div>", "class=\"stui-vodlist__thumb&&/a>", "class=\"myui-vodlist__thumb&&/a>", "mo-situ-pics&&</li>", "class=\"hl-item-thumb&&</a>"};
                    for (int i=0; i<playArray.length; i++) {
                        jiequContents = subContent(parseContent, playArray[i], "");
                        if (jiequContents.get(0).length()>50) {
                            if (!oComand.contains("l") && !"首页".equals(activeCate) && !"搜索".equals(activeCate)) rule.put("数组", playArray[i]);
                            jiequshuzuqian = playArray[i];
                            break;
                        }
                    }
                    if (jiequshuzuqian.length()<1) {
                        if (!"搜索".equals(activeCate)) {
                            jiequshuzuqian = "<li*>&&</li>[不包含:首页#剧集#连续剧#电视剧#综艺#动漫#我想看#追剧#留言#APP#观看纪录#求片#福利#推荐]";
                        } else {
                            jiequshuzuqian = "<a&&</a>[不包含:首页#剧集#连续剧#电视剧#综艺#动漫#我想看#追剧#留言#APP#观看纪录#求片#福利#推荐]";
                        }
                    }
                } else {
                    jiequshuzuqian = "<a&&</a>[不包含:首页#剧集#连续剧#电视剧#综艺#动漫#我想看#追剧#留言#APP#观看纪录#求片#福利#推荐]";
                }
                if (jiequshuzuqian.startsWith("<")) {
                    jiequContents = subContent(parseContent, jiequshuzuqian, "");
                    jiequshuzuqian = "";
                }
            }
            JSONArray vodArray = null;
            if ((parseContent.startsWith("{") && parseContent.endsWith("}") && jiequshuzuqian.length()>1 && !jiequshuzuqian.contains("&&") && !jiequshuzuqian.contains("$$") && jiequshuzuhou.length()<1) || getRuleVal("cat_mode").equals("0")) {
                vodArray = getJsonArray(parseContent, jiequshuzuqian);
            } else if (parseContent.startsWith("[") && parseContent.endsWith("]")) {
                vodArray = new JSONArray(parseContent);
            }
            int listCount = 0;
            if (vodArray!=null && vodArray.length()>0) {
                listCount = vodArray.length();
                jsonMode = true;
            } else {
                listCount = jiequContents.size();
                jsonMode = false;
            }
            if (directPlayList != null) {
                directPlayList.clear();
            } else {
                directPlayList = new ArrayList<>();
            }
            int playListNum = 0;
            for (int i = 0; i < listCount; i++) {
                try {
                  String title = "", link = "", pic = "", remark = "";
                  if (jsonMode) {
                    String vod = vodArray.getJSONObject(i).toString();
                    if (titleV.contains("&&")) titleV = "name";
                    if (linkV.contains("&&")) linkV = "id";
                    if (picV.contains("&&")) picV = "cover";
                    if (remarkV.contains("&&")) remarkV = "score";
                    title = getJsonArrayString(vod, titleV);
                    title = title.replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").replaceAll("\\s+","");
                    link = getJsonArrayString(vod, linkV);
                    pic = getJsonArrayString(vod, picV);
                    if (pic.length()<1 || pic.equals("null"))
                        pic = getJsonArrayString(vod, "pic");
                    if (pic.length()<1 || pic.equals("null"))
                        pic = getJsonArrayString(vod, "img");
                    remark = getJsonArrayString(vod, remarkV);
                    if (link.length()<1 || (oComand.contains("k") && quick && key.length()>0 && !title.contains(key))) continue;
                    if (debug) title = "json:" + link;
                  } else {
                    String jiequContent = jiequContents.get(i);
                    if (jiequContent.equals("不要")) continue;
                    title = removeHtml(subContent(jiequContent, titleV, getRuleVal("biaotihou")).get(0)).trim();
                    if (title.isEmpty()) {
                        title = removeHtml(subContent(jiequContent, "alt=\"&&\"", "").get(0)).trim();
                    }
                        title = title.replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").replaceAll("\\s+","").replaceAll("立[即刻]播放","");
                    if (title.length()<1 || title.matches("\\S{1,2}页") || title.equals("不要") || title.equals("游客") || (title.contains("资源") && jiequshuzuqian.length()<1) || (oComand.contains("k") && quick && key.length()>0 && !title.contains(key))) continue;
                    String tupianqian = picV.toLowerCase();
                    if (tupianqian.startsWith("http://") || tupianqian.startsWith("https://")) {
                        pic = tupianqian;
                    } else {
                        pic = subContent(jiequContent, tupianqian, getRuleVal("tupianhou")).get(0);
                    }
                    if (pic.isEmpty()) {
                        pic = subContent(jiequContent, "src=\"&&\"", "").get(0);
                    }
                    if (pic.length()<6 && jiequshuzuqian.length()<1) continue;
                    link = subContent(jiequContent, linkV, getRuleVal("lianjiehou")).get(0).trim().replace(homeUrl, "");
                    if (link==null || link.equals("不要") || link.length()<4 || link.equals(linkV.trim().replace(homeUrl, "").replace("+", "")) || ((title.contains(link) ||  link.contains("script") || link.startsWith("http"))  && jiequshuzuqian.length()<1)) continue;
                    if (debug) title = "xb:" + link;
                    remark = subContent(jiequContent, remarkV, getRuleVal("fubiaotihou")).get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("^ *(.*)", "$1").replace("更新", "更").replaceAll("<[^>]*>", ",").replaceAll("[(/>)<]", "").replaceAll(",+", ",").replaceAll("\\s+","");
                    if (remark.length()<2) {
                        String temp = jiequContent.startsWith("<") ? jiequContent : "<" + jiequContent;
                        temp = temp.endsWith(">") ? temp : temp + ">";
                        temp = temp.replaceAll("<[^>]*?>", " ").replaceAll("[><]", "").replaceAll(" +", " ");
                        for (String str : temp.split(" ")) {
                            str = str.replaceAll("\\s+","");
                            if (str.length()>1 && !str.equals(title) && !str.contains("片") && !str.contains("影") && !str.contains("剧") && !str.contains("荐") && !str.contains("类") && !str.contains("网") && !str.contains("高分") && !str.contains("Movie") && !str.contains("The") && !str.contains("全部")) {
                                remark = str;
                                break;
                            }
                        }
                    }
                    if (remark.startsWith(",")) remark = remark.substring(1,remark.length());
                    if (remark.endsWith(",")) remark = remark.substring(0,remark.length()-1);
                  }
                    pic = Misc.fixUrl(webUrl, pic);
                    if(picProxy){
                       pic = fixCover(pic,webUrl);
                    }
                    String linkPre = "", linkSuf = "";
                    if (getRuleVal("链接前缀", "列表链接前缀", "ljqianzhui", "cat_prefix", "").length()>0) linkPre = subContent(html, getRuleVal("链接前缀", "列表链接前缀", "ljqianzhui", "cat_prefix", ""), "").get(0).trim();
                    if (getRuleVal("链接后缀", "列表链接后缀", "ljhouzhui", "cat_suffix", "").length()>0) linkSuf = subContent(html, getRuleVal("链接后缀", "列表链接后缀", "ljhouzhui", "cat_suffix", ""), "").get(0).trim();
                    link = linkPre.length()<1 || link.startsWith("http") ? link + linkSuf : linkPre + link + linkSuf;
                    if (!link.startsWith("http") && !link.startsWith("magnet") && !link.startsWith("/")) link = "/" + link;
                    remark = remark.length() > 16 ? remark.substring(0,16) : remark;
                    playListNum++;
                    if (!oComand.contains("k") && key.length()>0 && !title.contains(key)) title = title + "〔" + key + "〕";
                    JSONObject v = new JSONObject();
                    if (playDirect) {
                        if (shortVideo) {
                            v.put("vod_id", title + "$$$" + pic + "$$$" + link+ "$$$" + "shortVideo");
                            directPlayList.add(title + "$" + link);
                         } else {
                            v.put("vod_id", title + "$$$" + pic + "$$$" + link+ "$$$" + "playDirect");
                        }
                    } else {
                        v.put("vod_id", title + "$$$" + pic + "$$$" + link);
                    }
                    v.put("vod_name", title);
                    v.put("vod_pic", pic);
                    v.put("vod_remarks", remark);
                    videos.put(v);
                    if (isHome && videos.length()>=homeCount) {
                        break;
                    }
                } catch (Throwable th) {
                    th.printStackTrace();
                }
              }
            }
            JSONObject result = new JSONObject();
            if (!isHome && !"搜索".equals(activeCate) && CATEURL.contains("{catePg}")) {
                result.put("page", pg);
                result.put("pagecount", Integer.MAX_VALUE);
                result.put("limit", 90);
                result.put("total", Integer.MAX_VALUE);
            }
            result.put("list", videos);
            return result;
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return null;
    }

    private static String removeUnicode(String str) {
        Pattern pattern = Pattern.compile("(\\\\u(\\w{4}))");
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String full = matcher.group(1);
            String ucode = matcher.group(2);
            char c = (char) Integer.parseInt(ucode, 16);
            str = str.replace(full, c + "");
        }
        return str.replaceAll("\\\\", "");
    }

    String removeHtml(String text) {
        return Jsoup.parse(text).text();
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        JSONObject obj = category(tid, pg, filter, extend);
        return obj != null ? obj.toString() : "";
    }


    @Override
    public String detailContent(List<String> ids) {
        try {
            String[] idInfo = ids.get(0).split("\\$\\$\\$");
            if (idInfo.length==1) {
                return pushAgent.detailContent(ids);
            } else if (idInfo[2].contains("https://www.aliyundrive.com/s/")) {
                ids.set(0, idInfo[2]);
                return pushAgent.detailContent(ids);
            }
            playDirect = idInfo.length>3 && ("playDirect".equals(idInfo[3]) || "shortVideo".equals(idInfo[3]));
            if(getRuleVal("播放二次截取", "dtNode", "").startsWith("//") && !playDirect) return xpDetailContent(ids);
            String jqHtml = "";
            ArrayList<String> playList = new ArrayList<>();
            String webUrl = (idInfo[2].startsWith("http") || idInfo[2].startsWith("magnet")) ? idInfo[2] : homeUrl + idInfo[2];
           String html = webUrl.contains(";post") ? fetchPost(webUrl) : fetch(webUrl.split(";")[0]);
           String descHtml = html;
           boolean jsonMode = false;
           boolean isMagnet = false;
	       ArrayList<String> playFrom = new ArrayList<>();
           if (playDirect) {
             if (idInfo.length>4 && idInfo[4]!=null && idInfo[4].length()>0) {
                playList.add(idInfo[0] + "$" + idInfo[2] + "#" + idInfo[4]);
             } else if (directPlayList!=null && directPlayList.size()>0) {
                for (int i=0; i<directPlayList.size(); i++) {
                    if (idInfo[2].equals(directPlayList.get(i).split("\\$")[1])) {
                        directPlayList.remove(i);
                        break;
                    }
                }
                String idsTemp = ids.get(0) + "$$$" + TextUtils.join("#", directPlayList);
                ids.set(0, idsTemp);
                playList.add(idInfo[0] + "$" + idInfo[2] + "#" + TextUtils.join("#", directPlayList));
             } else {
                playList.add(idInfo[0] + "$" + idInfo[2]);
		     }
             if (idInfo[2].startsWith("magnet")) {
                 isMagnet = true;
             }
           } else {
             ArrayList<String> xlContents = new ArrayList<>();
             if (getRuleVal("跳转链接").length()<2) xlContents.add(webUrl);
             if (!getRuleVal("多线链接", "跳转链接", "").isEmpty()) {
               String xlparseContent1 = html;
               boolean isXlLink = true;
               if (!getRuleVal("多线二次截取", "跳转二次截取", "").isEmpty()) {
                 xlparseContent1 = subContent(html, getRuleVal("多线二次截取", "跳转二次截取", ""), "").get(0);
                 if (xlparseContent1.isEmpty() || xlparseContent1==null) isXlLink = false;
               }
               if (isXlLink) {
                ArrayList<String> xljiequContents1 = subContent(xlparseContent1, getRuleVal("多线数组", "跳转数组", ""), "");
                if (xljiequContents1.size()<20) {
                 for (int m = 0; m < xljiequContents1.size(); m++) {
                   String xlLink = subContent(xljiequContents1.get(m), getRuleVal("多线链接", "跳转链接", ""), "").get(0).trim();
                   if (xlLink.length()<6 || xlLink==null || xlLink.equals("不要")) continue;
                   String xlLinkPre = "", xlLinkSuf = "";
                    if (getRuleVal("多线链接前缀", "跳转链接前缀", "").length()>0) xlLinkPre = subContent(xljiequContents1.get(m), getRuleVal("多线链接前缀", "跳转链接前缀", ""), "").get(0).trim();
                    if (getRuleVal("多线链接后缀", "跳转链接后缀", "").length()>0) xlLinkSuf = subContent(xljiequContents1.get(m), getRuleVal("多线链接后缀", "跳转链接后缀", ""), "").get(0).trim();
                   xlLink = xlLinkPre.length()<1 || xlLink.startsWith("http") ? xlLink + xlLinkSuf : xlLinkPre + xlLink + xlLinkSuf;
                   xlLink = xlLink.startsWith("http") ? xlLink : homeUrl + xlLink;
                   xlContents.add(xlLink);
                 }
                }
               }
             }
             for (int n = 0; n < xlContents.size(); n++) {
                if (n>0) {
                   String htmlLink = xlContents.get(n);
                    html = htmlLink.contains(";post") ? fetchPost(htmlLink) : fetch(htmlLink.split(";")[0]);
                }
                boolean reverse = getRuleVal("倒序", "倒序播放", "epi_reverse", "0").equals("1") || (!oComand.contains("d0") &&oComand.contains("d"));
           boolean bfshifouercijiequ = !getRuleVal("播放二次截取", "bfjiequqian", "list_twice_pre", "").isEmpty();
           String parseContent = html;
           if (bfshifouercijiequ) {
                String jiequhou = getRuleVal("bfjiequhou", "list_twice_suf", "");
                parseContent = subContent(html, getRuleVal("播放二次截取", "bfjiequqian", "list_twice_pre", ""), jiequhou).get(0);
                if (parseContent.isEmpty()) parseContent = html;
           }
                String jiequshuzuqian = getRuleVal("播放数组", "bfjiequshuzuqian", "list_arr_pre", "");
                String jiequshuzuhou = getRuleVal("bfjiequshuzuhou", "list_arr_suf", "");
                boolean bfyshifouercijiequ = !getRuleVal("列表二次截取", "bfyshifouercijiequ", "epi_twice_pre", "").isEmpty();
                ArrayList<String> jiequContents = subContent(parseContent, jiequshuzuqian, jiequshuzuhou);
                if (jiequshuzuqian.length()<1) {
                    String[] playArray = {"hl-sort-list&&</ul>", "sort-list clearfix&&</ul>", "id=\"hl-plays-list&&</ul>", "id=\"con_playlist&&</ul>", "<ul class=\"stui-play__list&&</ul>", "<ul class=\"myui-play__list&&</ul>", "<ul class=\"stui-content__playlist&&</ul>", "<ul class=\"content__playlist&&</ul>", "<div class=\"stui-content__playlist&&</div>", "<ul class=\"myui-content__list&&</ul>", "<ul id=\"playsx\"&&</ul>"};
                    for (int i=0; i<playArray.length; i++) {
                        jiequContents = subContent(parseContent, playArray[i], "");
                        if (jiequContents.get(0).length()>50) {
                           if (!oComand.contains("l")) rule.put("播放数组", playArray[i]);
                           jiequshuzuqian = playArray[i];
                            break;
                        }
                    }
                }
                if (jiequshuzuqian.length()<1) {
                    jiequContents.set(0, parseContent);
                }
                JSONArray jsonArray = new JSONArray();
              if (parseContent.startsWith("{") && parseContent.endsWith("}") && jiequshuzuqian.length()>1 && !jiequshuzuqian.contains("&&") && !jiequshuzuqian.contains("$$") && getRuleVal("bfyjiequshuzuhou", "epi_arr_suf", "").length()<1) {
                JSONObject jsonObject = new JSONObject(getJsonArrayString(parseContent, jiequshuzuqian + ";json;"));
                Iterator<String> keys = jsonObject.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    playFrom.add(key);
                    Object jObj = jsonObject.get(key);
                    if (jObj instanceof JSONObject) {
                       jsonArray.put((new JSONArray()).put(jsonObject.getJSONObject(key)));
                    } else if (jObj instanceof JSONArray) {
                       jsonArray.put(jsonObject.getJSONArray(key));
                    } else {
                        playFrom.remove(playFrom.size()-1);
                    }
                }
            }
            int xlCount = 0;
            if (jsonArray.length()>0) {
                xlCount = jsonArray.length();
                jsonMode = true;
            } else {
                xlCount = jiequContents.size();
                jsonMode = false;
            }
                for (int i = 0; i < xlCount; i++) {
                    try {
                        JSONArray vodArray = null;
                        String parseJqContent = "";
                        int listCount = 0;
                        if (jsonMode) {
                            vodArray = jsonArray.getJSONArray(i);
                            if (vodArray!=null && vodArray.length()>0) {
                                listCount = vodArray.length();
                            } else {
                                jsonMode = false;
                            }
                        } 
                            String jiequContent = jiequContents.get(i);
                            parseJqContent = bfyshifouercijiequ ? subContent(jiequContent, getRuleVal("列表二次截取", "播放剧集二次截取", "bfyjiequqian", "epi_twice_pre", ""), getRuleVal("bfyjiequhou", "epi_twice_suf", "")).get(0) : jiequContent;
                            if (parseJqContent.isEmpty() || parseJqContent==null) parseJqContent = jiequContent;
                       String jqList = "", titleV = "", linkV = "", linkV2 = "";
                        if (jiequshuzuqian.length()<1) {
                           jqList = getRuleVal("播放列表", "播放剧集截取数组", "bfyjiequshuzuqian", "epi_arr_pre", "<a&&</a>[不包含:src=#original=#background#tab-item#节点#线路#福利#推荐#追剧#游客#留言#求片#影视#下载]");
                           titleV = getRuleVal("播放标题", "播放剧集标题", "bfbiaotiqian", "epi_title", ">&&</a>[包含:集#清#版#HD#BD#0P#原画#蓝光#字#正片#0p#TC#TS#DVD#CD#期#季#语#话#1#2#3#4#5#6#7#8#9#" + idInfo[0] + "]");
                           linkV = getRuleVal("播放链接", "播放剧集链接", "bflianjieqian", "epi_url", "href=\"&&\"[不包含:href=\"/\"#search#show#view#detail#juqing#type#script#Script#read#list#/hot/#index.html#/news/#email#/appxz/#/tags/#.xml#.js#=http#.app]");
                           linkV2 = "href='&&'[不包含:search#show#view#detail#juqing#type#script#Script#read#list#/hot/#index.html#/news/#email#/appxz/#/tags/#.xml#.js#=http#.app]";
                        } else {
                            jqList = getRuleVal("播放列表", "播放剧集截取数组", "bfyjiequshuzuqian", "epi_arr_pre", "<a&&</a>");
                            titleV = getRuleVal("播放标题", "播放剧集标题", "bfbiaotiqian", "epi_title", ">&&</a>");
                            linkV = getRuleVal("播放链接", "播放剧集链接", "bflianjieqian", "epi_url", "href=\"&&\"");
                        }
                       ArrayList<String> lastParseContents = subContent(parseJqContent, jqList, getRuleVal("bfyjiequshuzuhou", "epi_arr_suf", ""));
                       if (debug) jqHtml = lastParseContents.toString();
               if (listCount<1 && parseJqContent.startsWith("{") && parseJqContent.endsWith("}") && jqList.length()>1 && !jqList.contains("&&") && !jqList.contains("$$") && getRuleVal("bfyjiequshuzuhou", "epi_arr_suf", "").length()<1) {
                   vodArray = getJsonArray(parseJqContent, jqList);
               }
               if (listCount<1 && vodArray!=null && vodArray.length()>0) {
                   listCount = vodArray.length();
                   jsonMode = true;
               } else if (listCount<1) {
                   listCount = lastParseContents.size();
                   jsonMode = false;
               }
               List<String> vodItems = new ArrayList<>();
               int k;
               String xlNum = "", saveJuji = "暂存剧集数";
               for (int j = 0; j < listCount; j++) {
                  k = reverse ? listCount - 1 - j : j;
                  String title = "", link = "";
                  if (jsonMode) {
                    String vod = vodArray.getJSONObject(k).toString();
                    if (titleV.contains("&&")) titleV = "name";
                    if (linkV.contains("&&")) linkV = "url";
                    title = getJsonArrayString(vod, titleV);
                    title = title.replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").replaceAll("\\s+","");
                    link = getJsonArrayString(vod, linkV);
                    if (title.length()<1 || title.equals("null"))
                        title = getJsonArrayString(vod, "episode");
                    if (link.length()<1 || link.equals("null"))
                        link = getJsonArrayString(vod, "id");
                    if (link.length()<1) continue;
                    if (debug) title = "json:" + link;
                  } else {
                           if (lastParseContents.get(k).equals("不要")) continue;
                          title = subContent(lastParseContents.get(k)+"</a>", titleV, getRuleVal("bfbiaotihou")).get(0).replaceAll("\\s+", "");
                          if (title.equals("新")) title = "";
                           title = title.replaceAll("\\&[a-zA-Z0-9#]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").trim();
                            link = subContent(lastParseContents.get(k), linkV, getRuleVal("bflianjiehou")).get(0).trim().replace(homeUrl, "");
                            if (link.length()<2) link = subContent(lastParseContents.get(k), linkV2, "").get(0).trim().replace(homeUrl, "");
                            if (title.equals("不要") || title.length()<1 || link==null || link.length()<4 || ((title.contains(".php") || title.contains(link) || title.matches("上一?集") || title.matches("下一?集") || link.startsWith("http") || (link.contains("/video/") && !link.contains("play") && !link.contains("sid")) || (!link.startsWith("magnet:") && !link.contains("/"))) && jiequshuzuqian.length()<1)) continue;
                            if (debug) title = "xb:" + link;
                  }
                            if (link.startsWith("magnet:")) {
                                isMagnet = true;
                                vodItems = new ArrayList<>();
                                playList = new ArrayList<>();
                                vodItems.add(title + "$" + link);
                                break;
                            }
                            String bfLinkPre = "", bfLinkSuf = "";
                            if (getRuleVal("播放链接前缀", "播放剧集链接前缀", "bfqianzhui", "epiurl_prefix", "").length()>0) bfLinkPre = subContent(html, getRuleVal("播放链接前缀", "播放剧集链接前缀", "bfqianzhui", "epiurl_prefix", ""), "").get(0).trim();
                            if (getRuleVal("播放链接后缀", "播放剧集链接后缀", "bfhouzhui", "epiurl_suffix", "").length()>0) bfLinkSuf = subContent(html, getRuleVal("播放链接后缀", "播放剧集链接后缀", "bfhouzhui", "epiurl_suffix", ""), "").get(0).trim();
                            link = bfLinkPre.length()<1 || link.startsWith("http") ? link + bfLinkSuf : bfLinkPre + link + bfLinkSuf;
                            if (!link.startsWith("http") && !link.startsWith("magnet") && !link.startsWith("/")) link = "/" + link;
                            String linkTemp = link, juji = "";
                            if (!jsonMode && bfLinkSuf.isEmpty() && linkTemp.matches(".*[/-]\\d+-.*") && !linkTemp.contains("sid/")) {
                                if (getRuleVal("倒序", "倒序播放", "epi_reverse", "").isEmpty() && !oComand.contains("d") && !saveJuji.isEmpty() && vodItems.isEmpty()) {
                                    juji = linkTemp.replaceAll(".*[/-]\\d{1,2}-(?:nid-)?(?:num-)?(\\d{1,4})(?:\\.html)?.*","$1");
                                    if (juji!=null && juji.length()>0 && Integer.parseInt(juji)>1) {
                                      if (saveJuji.equals("暂存剧集数")) {
                                        j=-1;
                                        reverse = true;
                                        saveJuji = juji;
                                        continue;
                                      } else {
                                        if (Integer.parseInt(juji)>Integer.parseInt(saveJuji)) {
                                            j=-1;
                                            reverse = false;
                                            saveJuji = "";
                                            continue;
                                        }
                                      }
                                    }
                                }
                                linkTemp = linkTemp.replaceAll(".*[/-](\\d{1,2})-(?:nid-)?(?:num-)?\\d{1,4}(?:\\.html)?.*","$1");
                            } else if (!jsonMode && bfLinkSuf.isEmpty() && linkTemp.contains("sid/")) {
                                if (getRuleVal("倒序", "倒序播放", "epi_reverse", "").isEmpty() && !oComand.contains("d") && !saveJuji.isEmpty() && vodItems.isEmpty()) {
                                    juji = linkTemp.replaceAll(".*sid/\\d{1,2}/\\w{3}/(\\d{1,4}).*","$1");
                                    if (juji!=null && juji.length()>0 && Integer.parseInt(juji)>1) {
                                      if (saveJuji.equals("暂存剧集数")) {
                                        j=-1;
                                        reverse = true;
                                        saveJuji = juji;
                                        continue;
                                      } else {
                                        if (Integer.parseInt(juji)>Integer.parseInt(saveJuji)) {
                                            j=-1;
                                            reverse = false;
                                            saveJuji = "";
                                            continue;
                                        }
                                      }
                                    }
                                }
                                linkTemp = linkTemp.replaceAll(".*sid/(\\d{1,2})/.*","$1");
                            } else {
                                linkTemp = "";
                                if (jiequshuzuqian.length()<1 && vodItems.size()>0 && (vodItems.get(0).contains("sid/") || vodItems.get(0).matches(".*[/-]\\d{1,2}-.*"))) continue;
                            }
                            if (jiequshuzuqian.length()<1 && !jsonMode && bfLinkSuf.isEmpty() && linkTemp.length()>0 && !xlNum.equals(linkTemp) && !link.equals(linkTemp)) {
                               if (!vodItems.isEmpty()) {
                                  
                                  if (vodItems.get(0).contains("sid/") || vodItems.get(0).contains("play") || vodItems.get(0).contains("-")) {
                                    if (vodItems.size()>1) {
                                        for (int vi = vodItems.size() - 1; vi >= 0; vi--) {
                                           if (vodItems.get(vi).contains(idInfo[0]) && vodItems.size()>1) vodItems.remove(vi);
                                        }
                                    }
                                    playList.add(TextUtils.join("#", vodItems));
                                  }
                                  saveJuji = "暂存剧集数";
                                  vodItems = new ArrayList<>();
                               }
                               xlNum = linkTemp;
                            }
                            vodItems.add(title + "$" + link);
                        }
                        if ((vodItems.get(0).contains("sid/") || vodItems.get(0).contains("play") || vodItems.get(0).contains("-")) && playList.size()>0 && !playList.get(0).contains("sid/") && !playList.get(0).contains("play") && !playList.get(0).contains("-")) playList.remove(0);
                        if (vodItems.size()>1) {
                            for (int vj = vodItems.size() - 1; vj >= 0; vj--) {
                               if (vodItems.get(vj).contains(idInfo[0]) && vodItems.size()>1) vodItems.remove(vj);
                            }
                        }
                        playList.add(TextUtils.join("#", vodItems));
                        if (isMagnet) {
                            break;
                            }
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }		  
                }
            }
                for (int i = playList.size() - 1; i >= 0; i--) {
                   if (playList.get(i).isEmpty()) playList.remove(i);
                }
                if (playList.size()>1) {
                    for (int pi = playList.size() - 1; pi >= 0; pi--) {
                       if (playList.get(pi).contains(idInfo[0]) && playList.size()>1) playList.remove(pi);
                    }
                }
           }
		    html = descHtml;
            String cover = idInfo[1], title = idInfo[0], desc = "", category = "", area = "", year = "", remark = "", director = "", actor = "";
            String descV = getRuleVal("简介", "播放页剧情", "juqingqian", "proj_plot", "述了&&</div>"), categoryV = getRuleVal("影片类型", "leixinqian", "类型：&&</div>"), areaV = getRuleVal("影片地区", "地区：&&</div>"), yearV = getRuleVal("影片年代", "niandaiqian", "年份：&&</div>"), remarkV = getRuleVal("影片状态", "状态", "播放页状态", "zhuangtaiqian", "状态：&&</div>"), directorV = getRuleVal("导演", "播放页导演", "daoyanqian", "导演：&&</div>"), actorV = getRuleVal("主演", "演员", "播放页演员", "zhuyanqian", "proj_actor", "主演：&&</div>");

            if(picProxy){
                cover = fixCover(cover,webUrl);
            }
           boolean bfshifouercijiequ = !getRuleVal("播放二次截取", "bfjiequqian", "list_twice_pre", "").isEmpty();
                   String parseContent = bfshifouercijiequ ? subContent(html, getRuleVal("播放二次截取", "bfjiequqian", "list_twice_pre", ""), "").get(0) : html;
                  if (parseContent.startsWith("{") && parseContent.endsWith("}") && !desc.contains("&&") && !actor.contains("&&") && !director.contains("&&") && !remark.contains("&&") && !area.contains("&&") && !year.contains("&&")) {
                    String jiequhou = getRuleVal("bfjiequhou", "list_twice_suf", "");
                    category = getJsonArrayString(parseContent, categoryV);
                    remark = getJsonArrayString(parseContent, remarkV);
                    desc = getJsonArrayString(parseContent, descV);
                    year = getJsonArrayString(parseContent, yearV);
                    area = getJsonArrayString(parseContent, areaV);
                    director = getJsonArrayString(parseContent, directorV);
                    actor = getJsonArrayString(parseContent, actorV);
          } 
          if (desc.length()<1 && actor.length()<1 && category.length()<1 && remark.length()<1 && year.length()<1 && director.length()<1 && area.length()<1) {
                    category = subContent(html, categoryV, getRuleVal("leixinhou")).get(0).trim().replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", ",").replaceAll("[(/>)<]", "").replaceAll("，", ",").replaceAll(",+", ",").replaceAll("地区.*","").replaceAll("\\s+", "");
                    category = category.replaceAll("上映.*", "").replaceAll("更新.*", "").replaceAll("主演.*", "").replaceAll("状态.*", "").replaceAll("总集数.*", "").replaceAll("编剧.*", "").replaceAll("年代.*", "").replaceAll("年份.*", "").replaceAll("国家.*", "").replaceAll("导演.*", "").replaceAll("剧情.*", "").replaceAll("简介.*", "");
                    while (category.startsWith(",")) {
                        category = category.substring(1,category.length());
                    }
                    while (category.endsWith(",")) {
                        category = category.substring(0,category.length()-1);
                    }
                area = subContent(html, areaV, "").get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", " ").replaceAll("[(/>)<]", "").replaceAll("\\s+", " ").trim().split(" ")[0];
                year = subContent(html, yearV, getRuleVal("niandaihou")).get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", " ").replaceAll("[(/>)<]", "").replaceAll("\\s+", " ").trim().split(" ")[0];
                if (year.trim().isEmpty()) {
                    year = subContent(html, "年代：&&</div>", "").get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", " ").replaceAll("[(/>)<]", "").replaceAll("\\s+", " ").trim().split(" ")[0];
                }
                remark = subContent(html, remarkV, getRuleVal("zhuangtaihou")).get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", " ").replaceAll("[(/>)<]", "").replaceAll("\\s+", " ").trim().split(" ")[0];
                if (remark.trim().isEmpty()) {
                    remark = subContent(html, "更新：&&</div>", "").get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", " ").replaceAll("[(/>)<]", "").replaceAll("\\s+", " ").trim().split(" ")[0];
                }
                    actor = subContent(html, actorV, getRuleVal("zhuyanhou")).get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", ",").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll(",+", ",");
                    if (actor.isEmpty())
                        actor = subContent(html, "演员：&&</div>", "").get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", ",").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll(",+", ",");
                    if (actor.isEmpty())
                        actor = subContent(html, "主演：&&</p>", "").get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", ",").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll(",+", ",");
                    if (actor.isEmpty())
                        actor = subContent(html, "演员：&&</p>", "").get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", ",").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll(",+", ",");
                    if (actor.startsWith("：") || actor.startsWith(":")) actor = actor.substring(1,actor.length());
                    actor = actor.replaceAll("类型.*", "").replaceAll("导演.*", "").replaceAll("主演.*", "").replaceAll("上映.*", "").replaceAll("更新.*", "").replaceAll("总集数.*", "").replaceAll("编剧.*", "").replaceAll("状态.*", "").replaceAll("年代.*", "").replaceAll("年份.*", "").replaceAll("国家.*", "").replaceAll("地区.*", "").replaceAll("简介.*", "").replaceAll("剧情.*", "").replaceAll("立即播放.*", "").replaceAll("《.*", "").replaceAll("“.*", "").replaceAll("该片.*", "");
                    while (actor.startsWith(",")) {
                        actor = actor.substring(1,actor.length());
                    }
                    while (actor.endsWith(",")) {
                        actor = actor.substring(0,actor.length()-1);
                    }
 
                   director = subContent(html, directorV, getRuleVal("daoyanhou")).get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", ",").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll(",+", ",").trim();
                    if (director.isEmpty())
                    director = subContent(html, "导演：&&</p>", "").get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", ",").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll(",+", ",").trim();
                    if (director.startsWith("：") || director.startsWith(":")) director = director.substring(1,director.length());
                    director = director.replaceAll("主演.*", "").replaceAll("演员.*", "").replaceAll("类型.*", "").replaceAll("上映.*", "").replaceAll("更新.*", "").replaceAll("状态.*", "").replaceAll("总集数.*", "").replaceAll("编剧.*", "").replaceAll("年代.*", "").replaceAll("年份.*", "").replaceAll("国家.*", "").replaceAll("地区.*", "").replaceAll("剧情.*", "").replaceAll("简介.*", "").replaceAll("立即播放.*", "").replaceAll("《.*", "").replaceAll("语言.*", "");
                    while (director.startsWith(",")) {
                        director = director.substring(1,director.length());
                    }
                    while (director.endsWith(",")) {
                        director = director.substring(0,director.length()-1);
                    }

                    desc = subContent(html, descV, getRuleVal("juqinghou")).get(0).trim().replaceAll("<script.*?>.*?</script>", "").replaceAll("<style.*?>.*?</style>", "").replace("详情", "").replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll("\"?var.*", "").replaceAll("立即播放.*", "").replaceAll("播放.*", "").replaceAll("\"?热搜.*", "").replaceAll("热门.*", "").replaceAll("name=.*", "");
                if (desc.length()<10) {
                      desc = subContent(html, "概要&&</p>", "").get(0).trim().replaceAll("<script.*?>.*?</script>", "").replaceAll("<style.*?>.*?</style>", "").replace("详情", "").replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll("\"?var.*", "").replaceAll("立即播放.*", "").replaceAll("播放.*", "").replaceAll("\"?热搜.*", "").replaceAll("热门.*", "");
                }
                if (desc.length()<10) {
                      desc = subContent(html, "简介&&</p>", "").get(0).trim().replaceAll("<script.*?>.*?</script>", "").replaceAll("<style.*?>.*?</style>", "").replace("详情", "").replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll("\"?var.*", "").replaceAll("立即播放.*", "").replaceAll("播放.*", "").replaceAll("\"?热搜.*", "").replaceAll("热门.*", "");
                }
                if (desc.replaceAll("\\s+", "").length()<10) {
                    desc = subContent(html, "剧情&&</p>[不包含:首页]", "").get(0).trim().replaceAll("<script.*?>.*?</script>", "").replaceAll("<style.*?>.*?</style>", "").replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll("\"?var.*", "").replaceAll("立即播放.*", "").replaceAll("播放.*", "").replaceAll("\"?热搜.*", "").replaceAll("热门.*", "").replaceAll("大家都在.*", "").replace("介绍", "");
                }
                if (desc.replaceAll("\\s+", "").length()<15) {
                    desc = subContent(html, "<p*>&&</p>[不包含:热搜#热门#播放#本站#导演：#更新：#状态：#主演：#演员：#地区：#年份：#年代：#类型：]", "").get(0).trim().replaceAll("<script.*?>.*?</script>", "").replaceAll("<style.*?>.*?</style>", "").replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll("\"?var.*", "");
                }
                if (desc.replaceAll("\\s+", "").length()<10) {
                    desc = subContent(html, "介绍&&</p>[不包含:首页]", "").get(0).trim().replaceAll("<script.*?>.*?</script>", "").replaceAll("<style.*?>.*?</style>", "").replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").replaceAll("\\s+", "").replaceAll("\"?var.*", "").replaceAll("立即播放.*", "").replaceAll("播放.*", "").replaceAll("\"?热搜.*", "").replaceAll("热门.*", "").replace("不要","");
                }
                if (desc.startsWith("：") || desc.startsWith(":")) desc = desc.substring(1,desc.length());
             }
                    if (debugAt.equals("1")) {
                        title = "ids" + ids.get(0).toString();
                        desc = "list:" + playList.toString();
                        actor = "xbjq:" + html;
                    } else if (debug) {
                        title = "ids" + ids.get(0).toString();
                        if (debugInfo==null) debugInfo = "空";
                        if (debugInfo.length()>debugStart+50) {
                            desc = debugInfo.substring(debugStart);
                        } else {
                            desc = debugInfo.substring(debugInfo.length()-100);
                        }
                        actor = "xbjq:" + jqHtml;
                    }
                    
            JSONObject vod = new JSONObject();
            vod.put("vod_id", ids.get(0));
            vod.put("vod_name", title);
            vod.put("vod_pic", cover);
            vod.put("type_name", category);
            vod.put("vod_year", year);
            vod.put("vod_area", area);
            vod.put("vod_remarks", remark);
            vod.put("vod_director", director);
            vod.put("vod_actor", actor);
            vod.put("vod_content", desc);
            String xlparseContent = html;
            if (getRuleVal("线路合并", "不分线路", "0").equals("1")) {
                String plTemp = TextUtils.join("#", playList);
                playList = new ArrayList<>();
                playList.add(plTemp);
                playFrom.add(idInfo[0]);
            } else if (playFrom.size()<1) {
               String xljiequshuzuqian = getRuleVal("线路数组", "线路名截取数组", "xljiequshuzuqian", "线路名截取数组前", "tab_arr_pre", "");
               String xljiequqian = getRuleVal("线路二次截取", "线路名二次截取", "xljiequqian", "线路名截取前", "tab_twice_pre", "");
                if (!getRuleVal("线路二次截取", "线路名二次截取", "xljiequqian", "线路名截取前", "tab_twice_pre", "").isEmpty()) {
                   String xljiequhou = getRuleVal("xljiequhou", "线路名截取后", "tab_twice_suf", "");
                   xlparseContent = subContent(html, xljiequqian, xljiequhou).get(0);
               }
              String xljiequshuzuhou = getRuleVal("xljiequshuzuhou", "线路名截取数组后","tab_arr_suf", "").replaceAll("\\[排序.*?\\]", "");

               /**
               * 排序关键词
               */
               String hasFlag = xljiequshuzuqian.replaceAll(".*(\\[.*)", "$1");
               xljiequshuzuqian = xljiequshuzuqian.replaceAll("\\[排序.*?\\]", "");
               ArrayList<String> xljiequContents = null;
                  if (xljiequshuzuqian.length()<1) {
                     String[] guess2 = {"换线路&&</ul>", "选择播放源&&</ul>", "节点列表&&</ul>", "<ul*tab-title\"&&</ul>", "<ul class=\"nav nav-btn&&</ul>[不包含:首页#电影]", "\"playname\"&&</ul>", "from*list\"&&</ul>", "<dt&&</dt>", "play_source_tab&&</div>"};
                     xljiequContents = new ArrayList<>();
                     for (String g: guess2) {
                     ArrayList<String> tempList = subContent(xlparseContent, g, "");
                        for (int l=0; l<tempList.size(); l++) {
                          ArrayList<String> temp4 = subContent(tempList.get(l), "<a&&</a>", "");
                          
                          for (int m=0; m<temp4.size(); m++) {
                              String t = subContent(temp4.get(m)+"</a>", ">&&</a>", "").get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("\\s+", "").replaceAll("<[^>]*?>", "").replaceAll("[><]", "");
                              if (t.length()>0) xljiequContents.add(t);
                          }
                          if (xljiequContents.size()==playList.size()) break;
                          xljiequContents = new ArrayList<>();
                          String temp3 = tempList.get(l).startsWith("<") ? tempList.get(l) : "<" + tempList.get(l);
                          temp3 = temp3.endsWith(">") ? temp3 : temp3 + "<";
                          temp3 = temp3.replaceAll("\\s+", "").replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*?>", " ").replaceAll("[><]", "").replaceAll(" +", " ");
                          for (String str : temp3.split(" ")) {
                              str = str.replaceAll("\\s+", "");
                              if (str.length()>0) {
                                  xljiequContents.add(str);
                              }
                          }
                          if (xljiequContents.size()==playList.size()) break;
                          xljiequContents = new ArrayList<>();
                        }
                     if (xljiequContents.size()==playList.size()) break;
                     xljiequContents = new ArrayList<>();
                     }

                     if (xljiequContents.size()!=playList.size()) {
                        String[] guess = {"module-tab-item&&</small>", "module-tab-item&&</div>", "module-tab-item &&</a>", "tabindex=*\"tab\">&&<", "\"tab\"*>&&<[不包含:同]", "\"hl-text-site\">&&<[不包含:评分#微信#扫一扫]", "playfrom*>&&</div>", "channelname*>&&</a>", "tabs-play*>&&</span>", "swiper-slide*>&&</a>[不包含:首页#电影]", "=\"pull-left\"*>&&<", "pull-right\">&&</div>", "pay-url*>&&</a>", "<h3*>&&</h3>[不包含:正片#猜#热#熱#更#介#榜#情#链#表#荐#排#评#留言#讨论#记#同#最新#演#正在#href=#收藏#" + idInfo[0] + "]", "<h4*>&&</h4>[不包含:正片#猜#热#熱#更#介#榜#情#链#表#荐#排#评#留言#讨论#记#同#最新#演#正在#href=#收藏#" + idInfo[0] + "]", "<h2*>&&</h2>[不包含:正片#猜#热#熱#更#介#榜#情#链#表#排#评#留言#讨论#记#同#最新#演#正在#href=#收藏#" + idInfo[0] + "]"};
                        for (int j=0; j<guess.length; j++) {
                           ArrayList<String> temp = subContent(xlparseContent, guess[j], "");
                           if (temp.size()<playList.size()) continue;
                           ArrayList<String> temp2 = new ArrayList<>();

                           for (int k=0; k<temp.size(); k++) {
                               if ((temp2.size()+temp.size()-k)<playList.size()) break;
                               if (temp.get(k).equals("不要") || temp.get(k).trim().length()<1) continue;
                               temp2.add(temp.get(k));
                           }
                           if (temp2.size()==playList.size()) {
                               xljiequContents = temp2;
                               break;
                           }
                        }
                     }
                  } else {
                       xljiequContents = subContent(xlparseContent, xljiequshuzuqian, xljiequshuzuhou);
                  }
	           ArrayList<String> playFromTemp = new ArrayList<>();
               int pfSize = 0;
               for (int i = 0; i < xljiequContents.size(); i++) {
                   String xltitle ="";
                   if (xljiequshuzuqian.length()<1) {
                       xltitle = ("<" + xljiequContents.get(i)).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*?>", "").replaceAll("[><]", "").replaceAll("\\s+", "");
                   } else {
                       xltitle = subContent(xljiequContents.get(i)+"</a>", getRuleVal("线路标题", "xlbiaotiqian", "线路名标题", "线路名标题前", "tab_title", ">&&</a>[不包含:m3u8]").replaceAll("\\[排序.*?\\]", ""), getRuleVal("xlbiaotihou", "线路名标题后", "tab_title", "")).get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").replaceAll("\\s+", "");
                   }
                   xltitle = xltitle.replaceAll(".*选择播放源", "").replaceAll(".*节点列表", "");
                      if (xltitle.equals("不要") || xljiequContents.get(i).equals("不要")) {
                            if (playList.size()==1) {
                               xltitle = xltitle.replace("不要", "保留线路");
                               playFromTemp.add(xltitle);
                               break;
                            } else {
                                   playList.remove(pfSize);
                                   continue;
                           }
                      }
                     if (xltitle.length()<1) continue;
                     xltitle = tH(hasFlag, xltitle, xlparseContent);
                     playFromTemp.add(xltitle);
                     pfSize++;
                     if (pfSize==playList.size()) break;
               }
               
               if (playFromTemp==null || playFromTemp.size()<1) {
                       for (int i = 0; i < playList.size(); i++) {
                           String xltitle1 = "线路" + (i + 1);
                           xltitle1 = tH(hasFlag, xltitle1, xlparseContent);
                           playFrom.add(xltitle1);
                       }
               }

               /*
                *线路重名处理
                */
               String[] fromTemp = playFromTemp.toArray(new String[playFromTemp.size()]);
               int num;
               boolean first;
               for (int i=0; i<fromTemp.length; i++) {
                   first = true;
                   num = 1;
                   for (int j=i+1; j<fromTemp.length; j++) {
                       String suf = num==1 ? "" : "1";
                       if ((fromTemp[j]+suf).equals(fromTemp[i])) {
                           if (first) {
                               fromTemp[i] = fromTemp[i] + num;
                               first = false;
                           }
                           num++;
                           fromTemp[j] = fromTemp[j] + num;
                       }
                   }
               }
               for (int i=0; i<fromTemp.length; i++) {
                   playFrom.add(fromTemp[i]);
               }


               /*
                *线路排序
                */
               if (hasFlag.length()>0) {
                    String hasWords = hasFlag.replaceAll(".*\\[排序:(.*?)\\].*", "$1");
                    if (!hasWords.isEmpty() && playFrom.size()>1) {
                        playFromTemp = new ArrayList<>();
                        ArrayList<String> playListTemp = new ArrayList<>();
                        for (String hasW: hasWords.split(">")) {
                            int pfS = playFrom.size();
                            for (int i = 0; i < pfS; i++) {
                                if (playFrom.get(i).contains(hasW)) {
                                    playFromTemp.add(playFrom.get(i));
                                    playListTemp.add(playList.get(i));
                                    playFrom.remove(i);
                                    playList.remove(i);
                                    break;
                                }
                            }
                            if (playFrom.size()==0)
                                break;
                        }
                        if (playFrom.size()>0) {
                            for (int i = 0; i < playFrom.size(); i++) {
                                playFromTemp.add(playFrom.get(i));
                                playListTemp.add(playList.get(i));
                            }
                        }
                        playFrom = new ArrayList<>();
                        playList = new ArrayList<>();
                        for (int i = 0; i < playFromTemp.size(); i++) {
                            playFrom.add(playFromTemp.get(i));
                            playList.add(playListTemp.get(i));
                        }
                    }
               }
           }
            String vod_play_from = TextUtils.join("$$$", playFrom);
            String vod_play_url = TextUtils.join("$$$", playList);
            if (!playDirect) {
                vod.put("vod_play_from", vod_play_from);
                vod.put("vod_play_url", vod_play_url);
            } else {
                vod.put("vod_play_from", "直播列表");
                vod.put("vod_play_url", playList.get(0));
            }
            JSONObject result = new JSONObject();
            JSONArray list = new JSONArray();
            list.put(vod);
            result.put("list", list);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    public String xpDetailContent(List<String> ids) {
        try {
            String[] idInfo = ids.get(0).split("\\$\\$\\$");
            String webUrl = (idInfo[2].startsWith("http") || idInfo[2].startsWith("magnet")) ? idInfo[2] : homeUrl + idInfo[2];
            JXDocument doc = xpFetch(webUrl);
            String jiequqian = getRuleVal("播放二次截取", "dtNode", "");

            String cover = idInfo[1], title = idInfo[0], desc = "", category = "", area = "", year = "", remark = "", director = "", actor = "";
            if(picProxy){
                cover = fixCover(cover,webUrl);
            }
            String descV = getRuleVal("简介", "dtDesc", ""), categoryV = getRuleVal("影片类型", "dtCate", ""), areaV = getRuleVal("影片地区", "dtArea", ""), yearV = getRuleVal("影片年代", "dtYear", ""), remarkV = getRuleVal("影片状态", "dtMark", ""), directorV = getRuleVal("导演", "dtDirector", ""), actorV = getRuleVal("主演", "演员", "dtActor", "");
                  try {
                    JXNode vodNode = doc.selNOne(jiequqian);
                    category = vodNode.selOne(categoryV).asString().trim();
                    year = vodNode.selOne(yearV).asString().trim();
                    area = vodNode.selOne(areaV).asString().trim();
                    remark = vodNode.selOne(remarkV).asString().trim();
                    actor = vodNode.selOne(actorV).asString().trim();
                    director = vodNode.selOne(directorV).asString().trim();
                    desc = vodNode.selOne(descV).asString().trim();
                  } catch (Exception e) {
                    SpiderDebug.log(e);
                          }
            JSONObject vod = new JSONObject();
            vod.put("vod_id", ids.get(0));
            vod.put("vod_name", title);
            vod.put("vod_pic", cover);
            vod.put("type_name", category);
            vod.put("vod_year", year);
            vod.put("vod_area", area);
            vod.put("vod_remarks", remark);
            vod.put("vod_actor", actor);
            vod.put("vod_director", director);
            vod.put("vod_content", desc);

            ArrayList<String> playFrom = new ArrayList<>();
            String xljiequshuzuqian = getRuleVal("线路数组", "dtFromNode", "");
               List<JXNode> fromNodes = doc.selN(xljiequshuzuqian);
                  for (int i = 0; i < fromNodes.size(); i++) {
                     String name = fromNodes.get(i).selOne(getRuleVal("线路标题", "dtFromName", "/text()")).asString().trim();
                     playFrom.add(name);
                  }

            ArrayList<String> playList = new ArrayList<>();
                String detailN = getRuleVal("播放数组", "dtUrlNode", "");
                String detailSN = getRuleVal("播放列表", "dtUrlSubNode", "//a");
                detailSN = detailSN;
                String detailName = getRuleVal("播放标题", "dtUrlName", "/text()");
                String detailId = getRuleVal("播放链接", "dtUrlId", "/@href");

               boolean isMagnet = false;
               List<JXNode> urlListNodes = doc.selN(detailN);
               for (int i = 0; i < urlListNodes.size(); i++) {
                 List<JXNode> urlNodes = urlListNodes.get(i).sel(detailSN);
                 List<String> vodItems = new ArrayList<>();
                 int k;
                 for (int j = 0; j < urlNodes.size(); j++) {
                    k = reverse ? urlNodes.size() - 1 - j : j;
                    String name = urlNodes.get(k).selOne(detailName).asString().trim();
                    String link = urlNodes.get(k).selOne(detailId).asString().trim();
                    if (link==null || link.length()<1) continue;
                    if (!link.startsWith("http") && !link.startsWith("magnet") && !link.startsWith("/")) link = "/" + link;
                    if (debug) name = "xp:" + link;
                    vodItems.add(name + "$" + link);
                    if (link.startsWith("magnet")) {
                        isMagnet = true;
                        break;
                    }
                 }
                // 排除播放列表为空的播放源
                if (vodItems.size() == 0 && playFrom.size() > i) {
                    playFrom.set(i, "");
                }
                playList.add(TextUtils.join("#", vodItems));
                if (isMagnet) break;
            }
            // 排除播放列表为空的播放源
            for (int i = playFrom.size() - 1; i >= 0; i--) {
                if (playFrom.get(i).isEmpty())
                    playFrom.remove(i);
            }
            for (int i = playList.size() - 1; i >= 0; i--) {
                if (playList.get(i).isEmpty())
                    playList.remove(i);
            }
            for (int i = playList.size() - 1; i >= 0; i--) {
                if (i >= playFrom.size())
                    playList.remove(i);
            }
            String vod_play_from = TextUtils.join("$$$", playFrom);
            String vod_play_url = TextUtils.join("$$$", playList);
            vod.put("vod_play_from", vod_play_from);
            vod.put("vod_play_url", vod_play_url);

            JSONObject result = new JSONObject();
            JSONArray list = new JSONArray();
            list.put(vod);
            result.put("list", list);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private String xhSubCut(String web, String endW) {
        if (endW.length()<0) return "";
        if (!endW.contains("*")) return xhEscape(endW);
        Pattern pattern = Pattern.compile(escapeExprSpecialWord(xhEscape(endW.split("\\*")[0])) + "([\\S\\s]*?)" + escapeExprSpecialWord(xhEscape(endW.split("\\*")[1])));
        Matcher matcher = pattern.matcher(web);
        while (matcher.find()) {
            return matcher.group(1); 
        }
        return "";
    }

    private String tH(String hasFlag, String thTemp, String web) {
                String num = thTemp.replaceAll(".*<序号>(.*)", "$1");
                thTemp = thTemp.replaceAll("<序号>.*", "");
                if (hasFlag.contains("替换:")) {
                    String thWords = hasFlag.replaceAll(".*\\[仅?替换:(.*?)\\].*", "$1");
                    thWords = khEscape(thWords);
                    thWords = thWords.replace("<序号>", num);
                    if (!thWords.isEmpty()) {
                        for (String thW: thWords.split("#")) {
                            thW = jhEscape(thW);
                            String startW = "", endW = "";
                            if (!thW.contains(">>>")) {
                                startW = thW.split(">>")[0];
                                endW = thW.split(">>")[1];
                            } else {
                                startW = thW.split(">>>")[0] + ">";
                                endW = thW.split(">>>")[1];
                            }
                            String temp = xhSubCut(web, endW);
                            if (startW.contains("*") && temp.length()>0) {
                                temp = temp.equals("空") ? "" : temp;
                                String sStartW = escapeExprSpecialWord(xhEscape(startW.split("\\*")[0]));
                                String eStartW = escapeExprSpecialWord(xhEscape(startW.split("\\*")[1]));
                                thTemp = thTemp.replaceAll(escapeExprSpecialWord(sStartW) + "([\\S\\s]*?)" + escapeExprSpecialWord(eStartW), temp);
                            } else if (temp.length()>0) {
                                temp = temp.equals("空") ? "" : temp;
                                thTemp = thTemp.replaceAll(escapeExprSpecialWord(xhEscape(startW)), temp);
                            }
                        }
                    }
                }
                return thTemp;
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            if (id.contains("https://www.aliyundrive.com/s/")) return pushAgent.playerContent(flag, id, vipFlags);
            JSONObject result = new JSONObject();
            String webUrl = (id.startsWith("http") || id.startsWith("magnet")) ? id : homeUrl + id;
            if (bt) {//过盾
                String html = OkHttpUtil.string(webUrl, getHeaders(webUrl));
                if (html.contains("检测中") && html.contains("跳转中") && html.contains("btwaf")) {
                    String btwaf = subContent(html, "btwaf=&&\"", "").get(0);
                    webUrl = webUrl + (webUrl.contains("?") ? "&" : "?") + "btwaf=" + btwaf;
                }
            }
            boolean mac = getRuleVal("免嗅", "mac", "Anal_MacPlayer", "0").equals("1") || oComand.contains("m");
            String twoLinkCut = getRuleVal("播放链接二次截取");
            if (oComand.contains("图图")) {
                String ttCode = "UTF-8", ttKey = "SRK#e%4UYtU#KiEo*vsPqpr0cC4bxAQW", ttIv = "o6sYmm*x5hn#rcCt";
                if (!isVideoFormat(webUrl)) {
                    String ttUrl = webUrl + "&sc=1&token=" + getToken(Long.valueOf(new Date().getTime()).toString(), ttCode, ttKey, ttIv);
                    webUrl = new JSONObject(decrypt(OkHttpUtil.string(ttUrl, getHeaders(ttUrl)), ttCode, ttKey, ttIv)).getString("url");
                }
                result.put("parse", 0);
            } else if (twoLinkCut.length()>0 || oComand.contains("e")) {
                twoLinkCut = twoLinkCut.length()>0 ? twoLinkCut : "url";
                String twoLinkJson =  fetch(webUrl);
                if (twoLinkJson.startsWith("{") && twoLinkJson.endsWith("}")) {
                    JSONObject twoLink = new JSONObject(twoLinkJson);
                    String eLink = twoLink.optString(twoLinkCut).trim();
                    if (eLink.length()>10) {
                        if (isVideoFormat(eLink)) {
                            webUrl = eLink;
                            result.put("parse", 0);
                        } else if (Misc.isVip(eLink)) {
                            webUrl = eLink;
                            result.put("parse", 1);
                            result.put("jx", "1");
                        } else {
                            result.put("parse", 1);
                        }
                    }
                }
            } else if (mac && !webUrl.startsWith("magnet")) {
                String videoUrl = null;
                Document doc = Jsoup.parse(OkHttpUtil.string(webUrl, getHeaders(webUrl)));
                Elements allScript = doc.select("script");
                for (int i = 0; i < allScript.size(); i++) {
                    String scContent = allScript.get(i).html().trim();
                    if (scContent.startsWith("var player_")) {
                        int start = scContent.indexOf('{');
                        int end = scContent.lastIndexOf('}') + 1;
                        String json = scContent.substring(start, end);
                        JSONObject player = new JSONObject(json);
                        String videoUrlTmp = player.getString("url");
                        if (player.has("encrypt")) {
                            int encrypt = player.getInt("encrypt");
                            if (encrypt == 1) {
                                videoUrlTmp = URLDecoder.decode(videoUrlTmp);
                            } else if (encrypt == 2) {
                                videoUrlTmp = new String(Base64.decode(videoUrlTmp, Base64.DEFAULT));
                                videoUrlTmp = URLDecoder.decode(videoUrlTmp);
                            }
                        }
                        videoUrl = videoUrlTmp;
                        break;
                    }
                }
                if (isVideoFormat(videoUrl)) {
                    webUrl = videoUrl;
                    result.put("parse", 0);
                } else if (Misc.isVip(videoUrl)) {
                    webUrl = videoUrl;
                    result.put("parse", 1);
                    result.put("jx", "1");
                } else {
                    result.put("parse", 1);
                }
            } else if (getRuleVal("强制解析").equals("1") || Misc.isVip(webUrl) || oComand.contains("j")) {
                result.put("parse", 1);
                result.put("jx", "1");
            }
            result.put("playUrl", getRuleVal("playUrl"));
            result.put("url", webUrl);
            String user = getPlayHeaders();
            if (user.length()>0) result.put("header",user);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    @Override
    public String searchContent(String key, boolean quick) {
        try {
            String sousuoqian = getRuleVal("搜索url", "搜索前", "sousuoqian", "search_url", "searchUrl", "");
            String webUrlTmp = "";
            String html = fetch(homeUrl);
            if (sousuoqian.length()<1) {
                sousuoqian = subContent(html, "<form*action=\"&&\"", "").get(0).trim();
                if (sousuoqian.length()>1) {
                    String method = subContent(html, "<form*method=\"&&\"", "").get(0).trim();
                    if ("post".equals(method)) {
                        sousuoqian = sousuoqian + ";post;";
                    } else {
                        sousuoqian = sousuoqian.indexOf("?")>0 ? sousuoqian + "&" : sousuoqian + "?";
                    }
                    sousuoqian = sousuoqian + subContent(html, "<input*name=\"&&\"", "").get(0).trim() + "={wd}";
                    sousuoqian = !sousuoqian.startsWith("/") && !sousuoqian.startsWith("http") && !sousuoqian.startsWith("magnet") ? "/" + sousuoqian : sousuoqian;
                }
            }
            String pgNum = "";
            if (webUrlTmp.matches(".*\\{pg\\d{2,}\\}.*")) {
                pgNum = webUrlTmp.replaceAll(".*\\{pg(\\d{2,})\\}.*", "$1");
                webUrlTmp = webUrlTmp.replace(pgNum, "");
            }
            if (key.contains("PG")) {
                pgNum = key.split("PG")[1];
                key = key.split("PG")[0];
            } else if (key.contains("pg")) {
                pgNum = key.split("pg")[1];
                key = key.split("pg")[0];
            }
            if (!sousuoqian.contains("{wd}")) {
                webUrlTmp = sousuoqian + key + getRuleVal("搜索后", "sousuohou");
            } else {
                webUrlTmp = sousuoqian.replace("{wd}", key);
            }
            if (!webUrlTmp.startsWith("http")) webUrlTmp = homeUrl + webUrlTmp;

            boolean ssmoshiJson = sousuoqian.contains("suggest") || sousuoqian.length()<2 || getRuleVal("搜索模式", "ssmoshi","").equals("0");
            JSONObject result = null;
            JSONArray videos = new JSONArray();
            if (!ssmoshiJson) {
              int pg = 1, pgEnd = 11;
              if (pgNum.length()>1 && pgNum.matches("\\d{2,}")) {
                 if (pgNum.length()==2) {
                     pg = Integer.parseInt(pgNum);
                     pgEnd = pg + 10;
                 } else {
                     pg = Integer.parseInt(pgNum.substring(0,2));
                     pgEnd = pg + Integer.parseInt(pgNum.substring(2));
                 }
              }
              int videosNum = 0;
              if (getRuleVal("搜索数组", "搜索截取数组", "ssjiequshuzuqian", "sea_arr_pre", "").length()<1) {
                  webUrlTmp = quick ? webUrlTmp + "##" + key + "quick" : webUrlTmp + "##" + key;
                  activeCate = "搜索";
                  if (!webUrlTmp.contains("{pg}")) {
                      result = category(webUrlTmp, "", false, new HashMap<>());
                  } else {
                      int pgTemp = pg;
                      String urlTmp = webUrlTmp.replace("{pg}", "" + pg);
                      result = category(urlTmp, "", false, new HashMap<>());
                      JSONArray sVideos = result.getJSONArray("list");
                      if (sVideos!=null) {
                          while (result.getJSONArray("list").length()>3 && pg<pgEnd) {
                              urlTmp = webUrlTmp.replace("{pg}", "" + ++pg);
                              result = category(urlTmp, "", false, new HashMap<>());
                              JSONArray v = result.getJSONArray("list");
                              if (v.length()>0) {
                                  for (int i=0; i<v.length(); i++) {
                                      sVideos.put(v.get(i));
                                  }
                              }
                          }
                          result = (new JSONObject()).put("list", sVideos);
                      }
                  }
                  if (result!=null && result.getJSONArray("list").length()>0) {
                      if (getRuleVal("搜索url", "搜索前", "sousuoqian", "search_url", "searchUrl", "").length()<1) rule.put("搜索url", sousuoqian);
                      return result.toString();
                  }
                  result = null;
                  ssmoshiJson = true;
              } else {
                  do {
                    String webUrl = webUrlTmp.replace("{pg}", String.valueOf(pg));
                    String webContent = "";
                    webContent = webUrlTmp.contains(";post") ? fetchPost(webUrl) : fetch(webUrl.split(";")[0]);
                    if (webContent.contains("没有找到")) return "";
                    webContent = webContent.replaceAll("热门电[\\S\\s]+", "").replaceAll("感兴趣[\\S\\s]+", "").replaceAll("热播影[\\S\\s]+", "");
                    String parseContent = webContent;
                    boolean shifouercijiequ = !getRuleVal("搜索二次截取", "ssjiequqian", "sea_twice_pre", "").isEmpty();
                    if (shifouercijiequ) {
                        String jiequqian = getRuleVal("搜索二次截取", "ssjiequqian", "sea_twice_pre", "");
                        String jiequhou = getRuleVal("ssjiequhou", "sea_twice_suf", "");
                        parseContent = subContent(webContent, jiequqian, jiequhou).get(0);
                        if (parseContent.isEmpty()) parseContent = webContent;
                    }
                    String jiequshuzuqian = getRuleVal("搜索数组", "搜索截取数组", "ssjiequshuzuqian", "sea_arr_pre", "<a&&</a>");
                    String jiequshuzuhou = getRuleVal("ssjiequshuzuhou", "sea_arr_suf", "");
                    String titleV = getRuleVal("搜索标题", "ssbiaotiqian", "sea_title", "title=\"&&\""), linkV = getRuleVal("搜索链接", "sslianjieqian", "sea_url", "href=\"&&\""), picV = getRuleVal("搜索图片", "sstupianqian", "sea_pic", "original=\"&&\""), remarkV = getRuleVal("搜索副标题", "ssfubiaotiqian", "sea_subtitle","");
                    ArrayList<String> jiequContents = subContent(parseContent, jiequshuzuqian, jiequshuzuhou);
                JSONArray vodArray = null;
                boolean jsonMode = false;
                if ((parseContent.startsWith("{") && parseContent.endsWith("}") && jiequshuzuqian.length()>1 && !jiequshuzuqian.contains("&&") && !jiequshuzuqian.contains("$$") && jiequshuzuhou.length()<1)) {
                    vodArray = getJsonArray(parseContent, jiequshuzuqian);
                }
                int listCount = 0;
                if (vodArray!=null && vodArray.length()>0) {
                    listCount = vodArray.length();
                    jsonMode = true;
                } else {
                    listCount = jiequContents.size();
                    jsonMode = false;
                }
    
                    for (int i = 0; i < listCount; i++) {
                        try {
                      String title = "", link = "", pic = "", remark = "";
                      if (jsonMode) {
                        String vod = vodArray.getJSONObject(i).toString();
                        if (titleV.contains("&&")) titleV = "name";
                        if (linkV.contains("&&")) linkV = "id";
                        if (picV.contains("&&")) picV = "pic";
                        if (remarkV.contains("&&")) remarkV = "score";
                        title = getJsonArrayString(vod, titleV);
                        link = getJsonArrayString(vod, linkV);
                        pic = getJsonArrayString(vod, picV);
                        if (pic.length()<1 || pic.equals("null"))
                            pic = getJsonArrayString(vod, "cover");
                        if (pic.length()<1 || pic.equals("null"))
                            pic = getJsonArrayString(vod, "img");
                        remark = getJsonArrayString(vod, remarkV);
                        if (link.length()<1) continue;
                      } else {
                            String jiequContent = jiequContents.get(i);
                         if (jiequContent.equals("不要") || jiequContent.isEmpty()) continue;
                           title = subContent(jiequContent, titleV, getRuleVal("ssbiaotihou")).get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").trim();
                         if (title.isEmpty())
                            title = removeHtml(subContent(jiequContent, "alt=\"&&\"", "").get(0)).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").trim();
                         if (title.isEmpty())
                            title = removeHtml(subContent(jiequContent, "span*>&&<", "").get(0)).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").trim();
                         if (title.isEmpty())
                            title = removeHtml(subContent(jiequContent+"</a>", ">&&</a>", "").get(0)).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").trim();
                            if (title.equals("不要") || title.isEmpty() || (quick && oComand.contains("k") && !title.contains(key))) continue;
                          if (picV.startsWith("http://") || picV.startsWith("https://")) {
                              pic = picV;
                          } else {
                             pic = subContent(jiequContent, picV, getRuleVal("sstupianhou")).get(0);
                            if (pic.isEmpty())
                                pic = subContent(jiequContent, "src=\"&&\"", "").get(0);
                            if (pic.equals("不要") || pic.isEmpty()) continue;
                          }
                            link = subContent(jiequContent, linkV, getRuleVal("sslianjiehou")).get(0).trim();
                            if (link.length()<4 || link==null || (getRuleVal("搜索数组").isEmpty() && link.startsWith("http") && !link.contains(homeUrl))) continue;
                            if (!remarkV.isEmpty()) {
                                remark = subContent(jiequContent, remarkV, getRuleVal("ssfubiaotihou")).get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("^ *(.*)", "$1").replace("更新", "更").replaceAll("<[^>]*>", ",").replaceAll("[(/>)<]", "").replaceAll(",+", ",");
                                remark = remark.length() > 16 ? remark.substring(0,16) : remark;
                             if (remark.startsWith(",")) remark = remark.substring(1,remark.length());
                             if (remark.endsWith(",")) remark = remark.substring(0,remark.length()-1);
                            }
                      }
                            pic = Misc.fixUrl(webUrl, pic);
                            if(picProxy){
                               pic = fixCover(pic,webUrl);
                            }
                        String linkPre = "", linkSuf = "";
                        if (getRuleVal("搜索链接前缀", "ssljqianzhui", "search_prefix", "").length()>0) linkPre = subContent(webContent, getRuleVal("搜索链接前缀", "ssljqianzhui", "search_prefix", ""), "").get(0).trim();
                        if (getRuleVal("搜索链接后缀", "ssljhouzhui", "search_suffix", "").length()>0 && getRuleVal("列表分类").length()<1) linkSuf = subContent(webContent, getRuleVal("搜索链接后缀", "ssljhouzhui", "search_suffix", ""), "").get(0).trim();
                        link = linkPre.length()<1 || link.startsWith("http") ? link + linkSuf : linkPre + link + linkSuf;
                        if (!link.startsWith("http") && !link.startsWith("magnet") && !link.startsWith("/")) link = "/" + link;
                        if (!oComand.contains("k") && !title.contains(key)) title = title + "〔" + key + "〕";
                            JSONObject v = new JSONObject();
                            v.put("vod_id", title + "$$$" + pic + "$$$" + link);
                            v.put("vod_name", title);
                            v.put("vod_pic", pic);
                            v.put("vod_remarks", remark);
                            videos.put(v);
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }
                    if (!webUrlTmp.contains("{pg}") || videosNum == videos.length()-1) break;
                    videosNum = videos.length();
                    pg++;
                  } while (pg < pgEnd && webUrlTmp.contains("{pg}"));
                }
                if (videos.length() < 1 ) ssmoshiJson = true;
            }
            if (ssmoshiJson) {
                if (!sousuoqian.contains("suggest"))
                    webUrlTmp = homeUrl + "/index.php/ajax/suggest?mid=1&wd=" + key + "&limit=500";
                String webUrl = webUrlTmp;
                String webContent = "";
                webContent = webUrlTmp.contains(";post") ? fetchPost(webUrl) : fetch(webUrl.split(";")[0]);
                JSONObject data = new JSONObject(webContent);
                JSONArray vodArray = data.getJSONArray("list");
                for (int j = 0; j < vodArray.length(); j++) {
                    JSONObject vod = vodArray.getJSONObject(j);
                    String name = vod.optString(getRuleVal("jsname", "jsonname", "name")).trim();
                    if (quick) {
                        if (!name.contains(key)) continue;
                    }
                    String id = vod.optString(getRuleVal("jsid", "jsonid", "id")).trim();
                    if ("".equals(id) || id==null || "".equals(name) || name==null) continue;
                    String pic = vod.optString(getRuleVal("jspic", "jsonpic", "pic")).trim();
                    pic = Misc.fixUrl(webUrl, pic);
                    if(picProxy){
                       pic = fixCover(pic,webUrl);
                    }
                    String ssljSuf = "";
                    if (getRuleVal("列表分类").length()>0) {
                        ssljSuf = getRuleVal("搜索链接后缀", "搜索连接后缀", "");
                    } else if (getRuleVal("搜索链接前缀", "search_prefix", "").replace(homeUrl, "").length()<1){
                        ssljSuf = getRuleVal("搜索后缀", "sousuohouzhui", "");
                    }
                    if (ssljSuf.length()<1) {
                        int homeCountTemp = homeCount;
                        homeCount = 2;
                        isHome = true;
                        JSONObject sufData = category("", "", false, new HashMap<>());
                        isHome = false;
                        homeCount = homeCountTemp;
                        ssljSuf = sufData.getJSONArray("list").getJSONObject(1).optString("vod_id").split("\\$\\$\\$")[2];
                        ssljSuf = ssljSuf.startsWith("http") ? ssljSuf.replace(homeUrl, "") : ssljSuf;
                        ssljSuf = ssljSuf.startsWith("/") ? ssljSuf : "/" + ssljSuf;
                        ssljSuf = ssljSuf.endsWith("/") ? ssljSuf.substring(0, ssljSuf.length()-1) : ssljSuf;
                        ssljSuf = ssljSuf.lastIndexOf("/")>0 ? ssljSuf.substring(0, ssljSuf.lastIndexOf("/")+1) : "";
                        if (getRuleVal("搜索后缀", "sousuohouzhui", "").length()<1) {
                            if (!ssljSuf.isEmpty()) {
                                rule.put("搜索后缀", ssljSuf);
                            } else {
                                rule.put("搜索后缀", "空");
                            }
                        }
                    }
                    String link = ssljSuf + id;
                    String linkPre = "", linkSuf = "";
                    if (getRuleVal("搜索链接前缀", "ssljqianzhui", "search_prefix", "").length()>0) linkPre = getRuleVal("搜索链接前缀", "ssljqianzhui", "search_prefix", "");
                    if (getRuleVal("搜索链接后缀", "ssljhouzhui", "search_suffix", "").length()>0 && getRuleVal("搜索前").length()<1) linkSuf = getRuleVal("搜索链接后缀", "ssljhouzhui", "search_suffix", "");
                    link = linkPre.length()<1 || link.startsWith("http") ? link + linkSuf : linkPre + link + linkSuf;
                    if (!link.startsWith("http") && !link.startsWith("magnet") && !link.startsWith("/")) link = "/" + link;
                    if (!oComand.contains("k") && !name.contains(key)) name = name + "〔" + key + "〕";
                    JSONObject v = new JSONObject();
                    v.put("vod_id", name + "$$$" + pic + "$$$" + link);
                    v.put("vod_name", name);
                    v.put("vod_pic", pic);
                    v.put("vod_remarks", "");
                    videos.put(v);
                }
            }
            result = new JSONObject();
            result.put("list", videos);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }
/*
    private static final byte[] UTF8_BOM_BYTES = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    static byte[] removeUTF8BOM(byte[] bytes) {
        boolean containsBOM = bytes.length > 3
                && bytes[0] == UTF8_BOM_BYTES[0]
                && bytes[1] == UTF8_BOM_BYTES[1]
                && bytes[2] == UTF8_BOM_BYTES[2];
        if (containsBOM) {
            byte[] copy = new byte[bytes.length - 3];
            System.arraycopy(bytes, 3, copy, 0, bytes.length - 3);
            return copy;
        }
        return bytes;
    }
*/
    private String deEnCode(String webUrl) {
        OKCallBack.OKCallBackDefault callBack = new OKCallBack.OKCallBackDefault() {
            @Override
            protected void onFailure(Call call, Exception e) {
            }
            @Override
            protected void onResponse(Response response) {
            }
        };
        if (webUrl.contains(";post")) {
            String postbody = webUrl.split(";post;")[1].trim();
            webUrl = webUrl.split(";")[0];
            LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
            String[] userbody = postbody.split("\\&");
            for (String userbd : userbody) {
                int loca = userbd.indexOf("=");
                params.put(userbd.substring(0,loca), userbd.substring(loca + 1));
            }
            if(!postbody.isEmpty() && postbody != null){
                OkHttpUtil.post(OkHttpUtil.defaultClient(), webUrl, params, getHeaders(webUrl), callBack);
            }else{
                OkHttpUtil.post(OkHttpUtil.defaultClient(), webUrl, null, getHeaders(webUrl), callBack);
            }
        } else {
            OkHttpUtil.get(OkHttpUtil.defaultClient(), webUrl, null, getHeaders(webUrl), callBack);
        }
        try {
            byte[] b = callBack.getResult().body().bytes();
            return new String(b, charSet);
        } catch (IOException e) {
        }
        return "";
    }

    protected JXDocument xpFetch(String webUrl) {
        String html = webUrl.contains(";post") ? fetchPost("xp"+webUrl) : fetch("xp"+webUrl.split(";")[0]);

        JXDocument doc = JXDocument.create(html);
        return doc;
    }

    protected String fetch(String webUrl) {
        SpiderDebug.log(webUrl);
        boolean xp = webUrl.startsWith("xp");
        webUrl = xp ? webUrl.replaceAll("xp(http.*)", "$1") : webUrl;
        String html = "";
        if (charSet.length()>0) {
            html = deEnCode(webUrl);
        }
        if (html.length()<1) {
            html = OkHttpUtil.string(webUrl, getHeaders(webUrl));
        }
        html = jumpbtwaf(webUrl, html);
        html = removeUnicode(html);

        return  html = xp ? html : html.replace(" ", "空空空").replaceAll("\\s", "").replace("空空空", " ");
    }

    protected String fetchPost(String webUrl) {
        SpiderDebug.log(webUrl);
        boolean xp = webUrl.startsWith("xp");
        webUrl = xp ? webUrl.replaceAll("xp(http.*)", "$1") : webUrl;
        String html = "";
        if (charSet.length()>0) {
            html = deEnCode(webUrl);
        }
        if (html.length()<1) {
            String postbody = webUrl.split(";post;")[1].trim();
            webUrl = webUrl.split(";")[0];
            OKCallBack.OKCallBackString callBack = new OKCallBack.OKCallBackString() {
                @Override
                protected void onFailure(Call call, Exception e) {
                }
                @Override
                protected void onResponse(String response) {
                }
            };
            if(!postbody.isEmpty() && postbody != null){
                if(postbody.startsWith("{") && postbody.endsWith("}")){
                  try {
                    JSONObject jsbody = new JSONObject(postbody);
                   OkHttpUtil.postJson(OkHttpUtil.defaultClient(), webUrl, jsbody.toString(), getHeaders(webUrl), callBack);
                  } catch (JSONException e) {
                  }
                }else{
                    LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
                    String[] userbody = postbody.split("\\&");
                    for (String userbd : userbody) {
                        int loca = userbd.indexOf("=");
                        params.put(userbd.substring(0,loca), userbd.substring(loca + 1));
                    }
                        OkHttpUtil.post(OkHttpUtil.defaultClient(), webUrl, params, getHeaders(webUrl), callBack);
                }
            }else{
                OkHttpUtil.post(OkHttpUtil.defaultClient(), webUrl, null, getHeaders(webUrl), callBack);
            }
            html = callBack.getResult();
        }
        html = jumpbtwaf(webUrl, html);
        html = removeUnicode(html);
        return html = xp ? html : html.replace(" ", "空空空").replaceAll("\\s", "").replace("空空空", " ");
    }

    private String getRuleVal(String key) {
        return getRuleVal(key, "");
    }

    private String getRuleVal(String key1, String key2, String defaultVal) {
        return getRuleVal(key1, getRuleVal(key2, defaultVal));
    }

    private String getRuleVal(String key1, String key2, String key3, String defaultVal) {
        return getRuleVal(key1, getRuleVal(key2, getRuleVal(key3, defaultVal)));
    }

    private String getRuleVal(String key1, String key2, String key3, String key4, String defaultVal) {
        return getRuleVal(key1, getRuleVal(key2, getRuleVal(key3, getRuleVal(key4, defaultVal))));
    }

    private String getRuleVal(String key1, String key2, String key3, String key4, String key5, String defaultVal) {
        return getRuleVal(key1, getRuleVal(key2, getRuleVal(key3, getRuleVal(key4, getRuleVal(key5, defaultVal)))));
    }

    private String getRuleVal(String key1, String key2, String key3, String key4, String key5, String key6, String defaultVal) {
        return getRuleVal(key1, getRuleVal(key2, getRuleVal(key3, getRuleVal(key4, getRuleVal(key5, getRuleVal(key6, defaultVal))))));
    }

    private String getRuleVal(String key, String defaultVal) {
        String v = rule.optString(key);
        if (key.equals("主页url")) {
          if (v.isEmpty()) {
            v = rule.optString("网站地址");           
            if (v.isEmpty()) {
              v = rule.optString("url");           
              if (v.isEmpty()) {
                v = rule.optString("homeUrl");           
                if (v.isEmpty()) {
                    v = rule.optString("分类url");
                    if (v.isEmpty()) {
                        v = rule.optString("分类页");
                        if (v.isEmpty()) { 
                            v = rule.optString("class_url");
                            if (v.isEmpty()) { 
                                v = rule.optString("cateUrl");
                                if (v.isEmpty()) { 
                                    v = rule.optString("搜索url");
                                }
                            }
                        }
                    }
                    v = v.replaceAll(".*(https?\\://[^/]+)/.*","$1");
                }
              }
            }
          }
        }
        if (key.equals("分类")) {
            if (!v.isEmpty()) {
                if (v.contains("&")) {
                    v = merge(v, rule.optString("分类值"));
                }
            } else {
                v = rule.optString("class_name");
                if (!v.isEmpty()) {
                    v = merge(v, rule.optString("class_value"));
                }
            }
        }
        if (v.isEmpty() || v.equals("空")) {
            if (key.equals("搜索后缀") && v.equals("空"))
                return "";
            return defaultVal;
        }
        if (!key.equals("剧情") && !key.equals("地区") && !key.equals("类型") && !key.equals("年份") && !key.equals("排序")) v = getActiveCut(v);
        return v;
    }

    private String getActiveCut(String cut) {
        if (!cut.contains("||") || !cut.contains("--")) return cut;
        String[] cutList = cut.split("\\|\\|");
        for (String cutWord: cutList) {
            if (cutWord.contains(activeCate)) {
                return cutWord.split("--")[1];
            }
        }
        if (cut.contains("||")) {
            String[] defList = cutList[0].split("--");
            if (defList.length>1) return defList[1];
            return defList[0];
        }
        return "";
    }

    private String getCate() {
        cateData = createCate().replace("電影", "电影").replace("連續劇", "连续剧").replace("電視劇", "电视剧").replace("劇集", "剧集").replace("動漫", "动漫").replace("綜藝", "综艺").replaceAll("\\s", "");
        return cateData;
    }

    private String createCate() {
        String cate = getRuleVal("分类");
        String numCate = "电影$1#连续剧$2#综艺$3#动漫$4";
         try {
            if (cate.contains("$")) {
                return cate;
            } else if (getRuleVal("分类数组").contains("&&") && !getRuleVal("分类数组").startsWith("//")) {
                String html = fetch(homeUrl);
                String jiequContent = html;
                if (!getRuleVal("分类二次截取").isEmpty()) {
                    jiequContent = subContent(html, getRuleVal("分类二次截取"), "").get(0);
                }
                if (jiequContent.isEmpty()) jiequContent = html;
                ArrayList<String> lastParseContents = subContent(jiequContent, getRuleVal("分类数组"), "");
                String cateItems = "";
                for (int i = 0; i < lastParseContents.size(); i++) {
                    if (lastParseContents.get(i).equals("不要")) continue;
                    String title = subContent(lastParseContents.get(i)+"</a>", getRuleVal("分类标题"), ">&&</a>").get(0).replaceAll("\\&#?[a-zA-Z0-9]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "").trim();
                    String id = subContent(lastParseContents.get(i), getRuleVal("分类ID"), "href=\"&&\"").get(0);
                    if (title.equals("不要") || title.isEmpty() || title==null || id.isEmpty() || id==null) continue;
                    if (debug) title = "xb:" + id;
                    cateItems = cateItems + title + "$" + id + "#";
                }
                return cateItems.substring(0,cateItems.length()-1);
            }
                String cateItems = "";
                if (!getRuleVal("cateManual").isEmpty()) {
                    JSONObject navs = rule.optJSONObject("cateManual");
                    if (navs != null) {
                        Iterator<String> keys = navs.keys();
                        while (keys.hasNext()) {
                            String name = keys.next();
                            cateItems = cateItems + name.trim() + "$" + navs.getString(name).trim() + "#";
                        }
                        cateItems = cateItems.substring(0,cateItems.length()-1);
                        return cateItems;
                    }
                }
                JXDocument doc = xpFetch(homeUrl);
                    if (cate.length()<1 && getRuleVal("分类数组").startsWith("//")) cate = getRuleVal("分类数组");
                    String ul = "ul", a = "a", cateM = "", index = "";
                    if  (cate.length()<1) {
                        cate = "萝莉";
                    } else if (!cate.startsWith("//") && cate.contains("\\.")) {
                        if (cate.split("\\.")[0].contains("[")) {
                            ul = cate.split("\\.")[0].split("\\[")[0];
                            index = "[" + cate.split("\\.")[0].split("\\[")[1];
                        } else {
                            ul = cate.split("\\.")[0];
                        }
                        a = cate.split("\\.")[1];
                    }
                    cateM = cate.startsWith("//") ? cate : "//" + ul + "[(contains(//text(),'" + cate + "') or contains(//text(),'连续剧') or contains(//@title,'連續劇') or contains(//text(),'电视剧') or contains(//@title,'電視劇') or contains(//text(),'剧集') or contains(//@title,'劇集') or contains(//text(),'无码') or contains(//@title,'无码') or contains(//text(),'無碼') or contains(//@title,'無碼') or contains(//text(),'国产') or contains(//@title,'國產') or contains(//text(),'亚洲') or contains(//@title,'亚洲') or contains(//text(),'亞洲') or contains(//@title,'亞洲')) and not(contains(//@data-original,'/') or contains(//@data-src,'/') or contains(//@src,'/') or contains(//@background,'/'))]" + index + "//" + a + "[not(contains(//text(),'页') or contains(//text(),'讯') or contains(//text(),'新') or contains(//text(),'追剧') or contains(//text(),'热搜') or contains(//text(),'榜单') or contains(//text(),'会员') or contains(//text(),'排行') or contains(//text(),'留言') or contains(//text(),'私人') or contains(//text(),'影院') or contains(//text(),'网') or contains(//text(),'影视') or contains(//text(),'联系') or contains(//text(),'专题') or contains(//text(),'明星') or contains(//text(),'角色') or contains(//text(),'图') or contains(//text(),'节目') or contains(//text(),'剧情') or contains(//text(),'韩娱') or contains(//text(),'演员') or contains(//text(),'文章') or contains(//text(),'其他') or contains(//text(),'音乐') or contains(//text(),'推荐') or contains(//text(),'APP') or contains(//text(),'下载'))]";
                    List<JXNode> navNodes = doc.selN(cateM);
                    for (int i = 0; i < navNodes.size(); i++) {
                        String name = navNodes.get(i).selOne(getRuleVal("分类标题", "cateName", "//text()")).asString().replaceAll("\\s","").trim();
                        if (name.length()<2) name = navNodes.get(i).selOne("/@title").asString().trim();
                        if (name.length()>9 || name.length()<2 || cateItems.contains(name) || (name.contains("直播") && !cate.contains("直播")) || (oComand.contains("!") && (name.contains("理") || name.contains("福") || name.contains("美女")))) continue;
                        String id = navNodes.get(i).selOne(getRuleVal("分类ID", "分类链接", "cateId", "/@href")).asString().trim();
                        if (id.contains("search")) {
                            if (!cateItems.contains("剧") && !cateItems.contains("劇")) cateItems = cateItems + name + "$" + name + "#";
                        continue;
                        }
                        if (id.startsWith("http")) id = id.replace(homeUrl, "");
                         if (id.length()<2 || !id.contains("/") || id.contains("detail") || id.contains("Detail") || id.contains("play")) continue;
                        String idTemp = id;
                        if (id.matches("/.*?[-_~/]\\d+[-_~/][10].*")) {
                            id = id.replaceAll("/.*?[/-_~/](\\d+)[[-_~/]][10].*", "$1");
                        }
                        if (id==null || !id.matches("\\d+")) {
                            id = idTemp;
                            id = id.matches(".*id[-_~/=].*") ? "/" + id.split("id[-_~/=]")[1] : id;
                            id = id.endsWith(".html") ? id.substring(0, id.length()-5) : id;
                            id = id.endsWith("K") ? id.substring(0, id.length()-1) : id;
                            id = id.endsWith("/1/index") ? id.substring(0, id.length()-6) : id;
                            id = id.endsWith("/index") ? id.substring(0, id.length()-6) : id;
                            id = id.endsWith("-1") ? id.substring(0, id.length()-2) : id;
                            id = id.endsWith("~1") ? id.substring(0, id.length()-2) : id;
                            if (!id.startsWith("http")) id = id.substring(id.lastIndexOf("/")+1, id.length());
                            id = id.replace("index", "").replace("-----------", "");
                            if (id.length()<1 || id.length()>21 || id.startsWith("http") || cateItems.contains(id)) continue;
                        }
                        if (debug) name = id;
                        cateItems = cateItems + name + "$" + id + "#";
                    }
                        if (cateItems.length()<6) return numCate;
                        cateItems = cateItems.substring(0,cateItems.length()-1);
                        return cateItems;
             
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return numCate;
    }

    private String merge(String n, String v) {
        if (v.equals("*") || v.isEmpty()) v = n;
        String[] nList = n.split("\\&");
        String[] vList = v.split("\\&");
        n = "";
        String suffix = "";
        for (int i=0; i<nList.length; i++) {
            suffix =  i < nList.length-1 ? "#": "";
            n = n + nList[i] + "$" + vList[i] + suffix;
        }
        return n;
    }

    private String getICAY(int i, String cN, String data) {
                    String icay = "0";
                    String cateName = ("#" + cateData + "#").replaceAll(".*#(.*?)\\$"+cN+"#.*", "$1");
                    if (!data.contains("||") && !data.contains("--")) {
                        icay = data;
                    } else {
                        if (!data.contains("--")) {
                            icay = cN + "--" + data.split("\\|\\|")[i];
                        } else {
                            if (!data.contains("||")) {
                               if (cateName.equals(data.split("--")[0])) icay = cN + "--" + data.split("--")[1];
                            } else {
                                for (String ca: data.split("\\|\\|")) {
                                   if (cateName.equals(ca.split("--")[0])) {
                                       icay = cN + "--" + ca.split("--")[1];
                                       break;
                                   }
                                }
                            }
                            if (icay.equals("0")) {
                                if (!data.contains("||")) {
                                    if (data.split("--")[0].equals(""+(i+1))) icay = cN + "--" + data.split("--")[1];
                                } else {
                                    for (String ca: data.split("\\|\\|")) {
                                       if (ca.split("--")[0].equals(""+(i+1))) {
                                       icay = cN + "--" + ca.split("--")[1];
                                           break;
                                       }
                                    }
                                }
                            }
                        }
                    }
                    return icay;
    }

    private JSONObject getFilterData() {
        try {
            String cateD = cateData;
            String tv = "连续剧", otherTv = "", otherMovie = "", otherO = "";
            ArrayList<String> cateNames = new ArrayList<>();
            String[] cateDataList = cateD.split("#");
            for (String cD: cateDataList) {
                cateNames.add(cD.split("\\$")[1]);
                if ("电视剧".equals(cD.split("\\$")[0])) {
                    tv = "电视剧";
                    otherTv = cD.split("\\$")[1];
                } else if ("电视".equals(cD.split("\\$")[0])) {
                    tv = "电视";
                    otherTv = cD.split("\\$")[1];
                } else if ("剧集".equals(cD.split("\\$")[0])) {
                    tv = "剧集";
                    otherTv = cD.split("\\$")[1];
                } else if ("连续剧".equals(cD.split("\\$")[0])) {
                    otherTv = cD.split("\\$")[1];
                } else if ("电影".equals(cD.split("\\$")[0])) {
                    otherMovie = cD.split("\\$")[1];
                } else if ("动漫".equals(cD.split("\\$")[0])) {
                    otherO = cD.split("\\$")[1];
                }
            }
            String cateId = getRuleVal("类型", "筛选子分类名称", "0");
            String cateUrl = CATEURL;
            String classData = getRuleVal("剧情", "筛选类型名称", "");
            String suffix = "";
            if ((cateUrl.contains("{class}") && classData.isEmpty()) || classData.contains("[替换")) {
                String classTemp = classData;
                classData = "电影--喜剧&爱情&动作&科幻&剧情&战争&警匪&犯罪&动画&奇幻&武侠&冒险&枪战&恐怖&悬疑&青春&古装&历史&运动&儿童&伦理||连续剧--古装&神话&战争&偶像&爱情&喜剧&家庭&犯罪&悬疑&恐怖&武侠&动作&奇幻&剧情&伦理&历史||综艺--脱口秀&真人秀&情感&旅游&音乐&舞蹈&美食&纪实&生活||动漫--科幻&热血&搞笑&冒险&校园&动作&运动||纪录片--纪录&历史&传记&音乐&歌舞&短片&科幻";
                if (!"连续剧".equals(tv)) classData = classData.replace("连续剧", tv);
                if(classTemp.contains("[替换")) {
                    classTemp = classTemp.replaceAll(".*\\[替换:(.*?)\\].*","$1");
                    for (String d: classTemp.split("#")) {
                        classData = classData.replace(d.split(">>")[0], d.split(">>")[1]);
                    }
                    classData = classData.replace("&&", "&");
                }
            }
            String areaData = getRuleVal("地区", "筛选地区名称", "");
            if ((cateUrl.contains("{area}") && areaData.isEmpty()) || areaData.contains("[替换")) {
                String areaTemp = areaData;
                areaData = "电影--大陆&香港&台湾&美国&法国&英国&日本&韩国&德国&泰国&印度&俄罗斯&意大利&西班牙&加拿大||连续剧--大陆&香港&台湾&美国&法国&英国&日本&韩国&德国&泰国&印度&俄罗斯&意大利&西班牙&加拿大||综艺--大陆&香港&台湾&日本&韩国&美国&英国||动漫--大陆&日本&韩国&美国&英国&法国||纪录片--大陆&香港&台湾&美国&法国&英国&日本&韩国&德国&泰国&印度&俄罗斯&意大利&西班牙&加拿大";
                if (!"连续剧".equals(tv)) areaData = areaData.replace("连续剧", tv);
                if(areaTemp.contains("[替换")) {
                    areaTemp = areaTemp.replaceAll(".*\\[替换:(.*?)\\].*","$1");
                    for (String d: areaTemp.split("#")) {
                        areaData = areaData.replace(d.split(">>")[0], d.split(">>")[1]);
                    }
                    areaData = areaData.replace("&&", "&");
                }
            }
            String yearData =  getRuleVal("年份", "筛选年份名称", "");
            if (cateUrl.contains("{year}") && yearData.isEmpty()) {
                int year = new Date().getYear() + 1900;
                yearData = year-15 + "-" + year;
            }
            String byData = getRuleVal("排序", "筛选排序名称", "");
            if (cateUrl.contains("{by}") && byData.isEmpty())
                byData = "时间$time#人气$hits#评分$score";
        JSONObject result = new JSONObject();
        JSONArray lists = new JSONArray();
            int i=0;
            for (String cN: cateNames) {
                String cNA = cN, cNC = cN, cdl = cateDataList[i].split("\\$")[0];
                if (cdl.contains("剧") && !cdl.contains("番剧")) {
                      cNC = otherTv;
                } else if ((cdl.contains("片") || cdl.contains("电影")) && !cdl.contains("纪录片") && !cdl.contains("动画片")) {
                      cNA = otherMovie;
                } else if (cdl.contains("番") || cdl.contains("动画") || cdl.contains("哔哩")) {
                      cNA = otherO;
                      cNC = otherO;
                }
                String cY = getICAY(i, cN, yearData);
                String cA = getICAY(i, cNA, areaData);
                String cC = getICAY(i, cNC, classData);
                String cI = getICAY(i, cN, cateId);
                lists = creatFilter(cI,cC,cA,cY,byData);
                result.put(cN,lists);
                i++;
            }
           return result;
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return null;
    }

    private JSONArray creatFilter(String cateI, String classD, String areaD, String yearD, String byD) {
        try {
            JSONArray lists = new JSONArray();
            if (cateI.contains("$")) {
                lists.put(getRType("cateId", "类型", cateI, ""));
            } else if (cateI.contains("&")) {
                lists.put(getRType("cateId", "类型", cateI, getRuleVal("类型值", "筛选子分类替换词", "")));
            }
            if (classD.contains("$")) {
                lists.put(getRType("class", "剧情", classD, ""));
            } else if (classD.contains("&")) {
                lists.put(getRType("class", "剧情", classD, getRuleVal("剧情值", "筛选类型替换词", "")));
            }
            if (areaD.contains("$")) {
                lists.put(getRType("area", "地区", areaD, ""));
            } else if (areaD.contains("&")) {
                lists.put(getRType("area", "地区", areaD, getRuleVal("地区值", "筛选地区替换词", "")));
            }
            if (yearD.contains("-") && !yearD.contains("--")) {
                int i = Integer.parseInt(yearD.split("-")[1]);
                int j = Integer.parseInt(yearD.split("-")[0]);
                if (j>i) {
                    int k = i;
                    i = j;
                    j = k;
                }
                String str = "";
                for (; i>=j; i--) {
                    if (i==j) {
                        str = str + String.valueOf(i);

                    } else {
                        str = str + String.valueOf(i) + "&";
                    }
                }
                yearD = str;
            }
            if (yearD.contains("&")) {
                lists.put(getRType("year", "年份", yearD, getRuleVal("年份值", "筛选年份替换词", "")));
            }
            if (byD.contains("$")) {
                lists.put(getRType("by", "排序", byD, ""));
            } else if (byD.contains("&")) {
                lists.put(getRType("by", "排序", byD, getRuleVal("排序值", "筛选排序替换词", "")));
            }
            return lists;

        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return null;
    }

    private JSONObject getRType(String key, String name, String typeD, String value) {
        try {
            JSONObject vType = new JSONObject();
            JSONArray lType = new JSONArray();
            if (!key.equals("by") && !key.equals("cateId")) {
                vType.put("n","全部");
                vType.put("v","");
                lType.put(vType);
                vType = new JSONObject();
                typeD = typeD.contains("--") ? typeD.split("--")[1] : typeD;
            }
            if (key.equals("cateId")) {
                vType.put("n","全部");
                vType.put("v",typeD.split("--")[0]);
                lType.put(vType);
                vType = new JSONObject();
                typeD = typeD.contains("--") ? typeD.split("--")[1] : typeD;
            }
            if (typeD.contains("#")) {
                if (typeD.contains("#")) {
                    for (String cD: typeD.split("#")) {
                        vType.put("n",cD.split("\\$")[0]);
                        vType.put("v",cD.split("\\$")[1]);
                        lType.put(vType);
                        vType = new JSONObject();
                    }
                } else {
                    vType.put("n",typeD.split("\\$")[0]);
                    vType.put("v",typeD.split("\\$")[1]);
                    lType.put(vType);
                }
            } else if (typeD.contains("&")) {
                String[] nameList = typeD.split("\\&");
                String[] valueList = "".equals(value) ? nameList : value.split("\\&");
                for (int i=0; i<nameList.length; i++) {
                    vType.put("n", nameList[i]);
                    vType.put("v", valueList[i]);
                    lType.put(vType);
                    vType = new JSONObject();
                }
            }
            JSONObject rType = new JSONObject();
            rType.put("key",key);
            rType.put("name",name);
            rType.put("value",lType);
            return rType;
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return null;
    }

    private String khEscape(String str) {
            if (str.contains("左括号")) str = str.replace("左括号", "[");
            if (str.contains("右括号")) str = str.replace("右括号", "]");
            return str;
    }

    private String jhEscape(String str) {
            if (str.contains("井号")) str = str.replace("井号", "#");
            return str;
    }

    private String xhEscape(String str) {
            if (str.contains("星号")) str = str.replace("星号", "*");
            return str;
    }

    private String ljEscape(String str) {
            if (str.contains("连接符")) str = str.replace("连接符", "&");
            return str;
    }

    private ArrayList<String> subContent(String content, String startFlag, String endFlag) {
        ArrayList<String> result = new ArrayList<>();
        if (startFlag.contains("Base64")) {
            startFlag = startFlag.replaceAll(".*Base64\\((.*?)\\).*","$1");
            if (startFlag.isEmpty() || startFlag==null) {
                result.add(new String(Base64.decode(content, Base64.DEFAULT)));
                return result;
            } else {
                result.add(new String(Base64.decode(subContentExt(content, startFlag, endFlag).get(0).trim(), Base64.DEFAULT)));
                return result;
            }
        }
        if (!startFlag.contains("+")) {
            return subContentExt(content, startFlag, endFlag);
        }
        String[] startFlagList = startFlag.split("\\+");
        String temp = "";
        for (int i=0; i<startFlagList.length; i++) {
            if (startFlagList[i].isEmpty()) continue;
            String sub = subContentExt(content, startFlagList[i], "").get(0).trim();
            if (sub.isEmpty()) continue;
            temp = temp + sub;
        }
        result.add(temp);
        return result;
    }

    private ArrayList<String> subContentExt(String content, String startFlag, String endFlag) {
        ArrayList<String> result = new ArrayList<>();
        if ((startFlag.isEmpty() && endFlag.isEmpty()) || startFlag.equals("空$$空") || startFlag.equals("空")) {
            result.add(content);
            return result;
        }
        try {
            if (!startFlag.contains("&&") && !startFlag.contains("$$") && startFlag.length()>0 && endFlag.length()<1) {
                if (startFlag.indexOf("[仅替换:")>=0) {
                    startFlag = tH(startFlag, content, content);
                }
                result.add(startFlag);
                return result;
            }
            if (startFlag.contains("\\[")) startFlag = startFlag.replace("\\[", "左括号");
            if (startFlag.contains("\\]")) startFlag = startFlag.replace("\\]", "右括号");
            if (startFlag.contains("\\*")) startFlag = startFlag.replace("\\*", "星号");
            if (startFlag.contains("\\&")) startFlag = startFlag.replace("\\&", "连接符");
            if (startFlag.contains("\\#")) startFlag = startFlag.replace("\\#", "井号");
            if (startFlag.contains("&&")) {
                if (startFlag.split("\\&\\&")==null || startFlag.split("\\&\\&").length<2) {
                    result.add(content);
                    return result;
                }
                endFlag = startFlag.split("\\&\\&")[1];
                startFlag = startFlag.split("\\&\\&")[0];
            } else if (startFlag.contains("$$")) {
                if (startFlag.split("\\$\\$")==null || startFlag.split("\\&\\&").length<2) {
                    result.add(content);
                    return result;
                }
                endFlag = startFlag.split("\\$\\$")[1];
                startFlag = startFlag.split("\\$\\$")[0];
            }
            startFlag = ljEscape(startFlag);
            endFlag = ljEscape(endFlag);
            String hasFlag = "";
            if (startFlag.contains("[")) {
                hasFlag = startFlag.replaceAll(".*(\\[.*)", "$1");
                startFlag = startFlag.replaceAll("\\[.*", "");
            }
            if (endFlag.contains("[")) {
                hasFlag = endFlag.replaceAll(".*(\\[.*)", "$1");
                endFlag = endFlag.replaceAll("\\[.*", "");
            }
            startFlag = khEscape(startFlag);
            endFlag = khEscape(endFlag);
            startFlag = jhEscape(startFlag);
            endFlag = jhEscape(endFlag);
            int n = 1;
            if (!startFlag.contains("*")) {
                startFlag = escapeExprSpecialWord(startFlag);
            } else {
                startFlag = escapeExprSpecialWord(xhEscape(startFlag.split("\\*")[0])) + "([^>]*?)" + escapeExprSpecialWord(xhEscape(startFlag.split("\\*")[1]));
                n = 2;
            }
            Pattern pattern = Pattern.compile(startFlag + "([\\S\\s]*?)" + escapeExprSpecialWord(endFlag));
            Matcher matcher = pattern.matcher(content);
            int cunt = 0;
            while (matcher.find()) {
                String thTemp = matcher.group(n);      
                cunt++;
                boolean has = true;
                String thTemp1 = thTemp + "<序号>" + cunt;
                thTemp = tH(hasFlag, thTemp1, content);
                if (hasFlag.contains("[包含:")) {
                    String hasWords = hasFlag.replaceAll(".*\\[包含:(.*?)\\].*", "$1");
                    hasWords = khEscape(hasWords);
                    if (!hasWords.isEmpty()) {
                        has = false;
                        for (String hasW: hasWords.split("#")) {
                            hasW = jhEscape(hasW);
                            hasW = xhEscape(hasW);
                            if (thTemp.contains(hasW)) {
                                has = true;
                                break;
                            }
                        }
                    }
                }
                if (!has) {
                    result.add("不要");
                    continue;
                } else if (hasFlag.contains("[不包含:")) {
                    String notWords = hasFlag.replaceAll(".*\\[不包含:(.*?)\\].*", "$1");
                    notWords = khEscape(notWords);
                    if (!notWords.isEmpty()) {
                        has = true;
                        for (String notW: notWords.split("#")) {
                            notW = jhEscape(notW);
                            notW = xhEscape(notW);
                            if (thTemp.contains(notW)) {
                                has = false;
                                break;
                            }
                        }
                    }
                }
                if (!has) {
                    result.add("不要");
                    continue;
                } else if (hasFlag.contains("[含序号:")) {
                    String hasWords = hasFlag.replaceAll(".*\\[含序号:(.*?)\\].*", "$1");
                    if (!hasWords.isEmpty()) {
                        has = false;
                        for (String hasW: hasWords.split("#")) {
                            if (hasW.contains("-")) {
                                String[] cuntWords = hasW.split("-");
                                for (int i = Integer.parseInt(cuntWords[0]); i <= Integer.parseInt(cuntWords[1]); i++) {
                                    if (i == cunt) {
                                        has = true;
                                        break;
                                    }
                                }
                            } else {
                                if (Integer.parseInt(hasW) == cunt) {
                                    has = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!has) {
                    result.add("不要");
                    continue;
                } else if (hasFlag.contains("[不含序号:")) {
                    String notWords = hasFlag.replaceAll(".*\\[不含序号:(.*?)\\].*", "$1");
                    if (!notWords.isEmpty()) {
                    has = true;
                        for (String notW: notWords.split("#")) {
                            if (notW.contains("-")) {
                                String[] cuntWords = notW.split("-");
                                for (int i = Integer.parseInt(cuntWords[0]); i <= Integer.parseInt(cuntWords[1]); i++) {
                                    if (i == cunt) {
                                        has = false;
                                        break;
                                    }
                                }
                            } else {
                                if (Integer.parseInt(notW) == cunt) {
                                    has = false;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!has) {
                    result.add("不要");
                    continue;
                } else {
                    result.add(thTemp);
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        if (result.isEmpty()) result.add("");
        return result;
    }

    protected String fixCover(String cover, String site) {
        try {
            return "proxy://do=xbpq&site="+site+"&pic=" + cover;
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return cover;
    }

    private static HashMap<String, String> picHeader = null;

    public static Object[] loadPic(Map<String, String> prmap) {
        try {
            //pic = new String(Base64.decode(pic, Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP), "UTF-8");
            String site = prmap.get("site");
            String pic = prmap.get("pic");

            if (picHeader == null) {
                picHeader = new HashMap<>();
                picHeader.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.54 Safari/537.36");
                picHeader.put("referer", site);
            }
            OKCallBack.OKCallBackDefault callBack = new OKCallBack.OKCallBackDefault() {
                @Override
                protected void onFailure(Call call, Exception e) {

                }

                @Override
                protected void onResponse(Response response) {

                }
            };
            OkHttpUtil.get(OkHttpUtil.defaultClient(), pic, null, picHeader, callBack);
            if (callBack.getResult().code() == 200) {
                Headers headers = callBack.getResult().headers();
                String type = headers.get("Content-Type");
                if (type == null) {
                    type = "application/octet-stream";
                }
                Object[] result = new Object[3];
                result[0] = 200;
                result[1] = type;
                System.out.println(pic);
                System.out.println(type);
                result[2] = callBack.getResult().body().byteStream();
                return result;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return null;
    }

    private String jumpbtwaf(String webUrl, String html) {
        if (html.contains("检测中") && html.contains("跳转中") && html.contains("btwaf")) {
            String btwaf = subContent(html, "btwaf=&&\"", "").get(0);
            String bturl = webUrl + (webUrl.contains("?") ? "&" : "?") + "btwaf=" + btwaf;
            html = fetch(bturl);
            bt = true;
        }
        return html;
    }

    String escapeExprSpecialWord(String regexStr) {
        if (!regexStr.isEmpty()) {
            String[] fbsArr = {"\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|"};
            for (String key : fbsArr) {
                if (regexStr.contains(key)) {
                    regexStr = regexStr.replace(key, "\\" + key);
                }
            }
        }
        return regexStr;
    }

    //修复软件不支持的格式无法嗅探的问题
    @Override 
    public boolean manualVideoCheck() { 
        return !getRuleVal("嗅探词", "过滤词", "").isEmpty() || getRuleVal("手动嗅探", "ManualSniffer").equals("1") || oComand.contains("s"); 
    } 
     
    @Override 
    public boolean isVideoFormat(String url) {
        url = url.toLowerCase();
        String[] videoFormatList = getRuleVal("嗅探词", getRuleVal("VideoFormat",".m3u8#.mp4#.flv#.mp3#.m4a")).split("#");
        String[] videoSniffList = getRuleVal("过滤词", getRuleVal("VideoFilter", "=http#.jpg#.png#.ico#.gif#.js")).split("#");
        for (String format : videoFormatList) {
            if (url.contains(format)) {
                for (String sniff : videoSniffList) {
                    if (url.contains(sniff)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public String decrypt(String strTime, String code, String key, String iv) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/CTR/PKCS5Padding");
            cipher.init(2, secretKeySpec, new IvParameterSpec(iv.getBytes()));
            return new String(cipher.doFinal(Base64.decode(strTime, 0)), code);
        } catch (Exception e) {
            return null;
        }
    }
    public String encrypt(String strTime, String code, String key, String iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CTR/PKCS5Padding");
            cipher.init(1, new SecretKeySpec(key.getBytes(), "AES"), new IvParameterSpec(iv.getBytes()));
            return Base64.encodeToString(cipher.doFinal(strTime.getBytes(code)), 0);
        } catch (Exception e) {
            return null;
        }
    }

    public String getToken(String strTime, String code, String key, String iv) {
        return encrypt(strTime, code, key, iv);
    }
}