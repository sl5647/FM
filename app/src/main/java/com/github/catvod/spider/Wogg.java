package com.github.catvod.spider;

import android.content.Context;
import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Utils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhixc
 */
public class Wogg extends Ali {
	
	private final String siteURL = "http://tvfan.xxooo.cf";
	
	private JSONObject filters;
	private final Pattern regexAli = Pattern.compile("(https://www.aliyundrive.com/s/[^\"]+)");
	private final Pattern regexCategory = Pattern.compile("/vodtype/(\\w+).html");
	private final Pattern regexPageTotal = Pattern.compile("\\$\\(\"\\.mac_total\"\\)\\.text\\('(\\d+)'\\);");
	
	private Map<String, String> getHeader() {
		Map<String, String> header = new HashMap<>();
		header.put("User-Agent", Utils.CHROME);
		return header;
	}
	
	@Override
	public void init(Context context, String extend) {
		super.init(context, extend);
		
		try {
			filters = new JSONObject("{\"1\": [{\"key\": \"3\", \"name\": \"剧情\", \"value\": [{\"n\": \"喜剧\", \"v\": \"喜剧\"}, {\"n\": \"爱情\", \"v\": \"爱情\"}, {\"n\": \"恐怖\", \"v\": \"恐怖\"}, {\"n\": \"动作\", \"v\": \"动作\"}, {\"n\": \"科幻\", \"v\": \"科幻\"}, {\"n\": \"剧情\", \"v\": \"剧情\"}, {\"n\": \"战争\", \"v\": \"战争\"}, {\"n\": \"警匪\", \"v\": \"警匪\"}, {\"n\": \"犯罪\", \"v\": \"犯罪\"}, {\"n\": \"动画\", \"v\": \"动画\"}, {\"n\": \"奇幻\", \"v\": \"奇幻\"}, {\"n\": \"武侠\", \"v\": \"武侠\"}, {\"n\": \"冒险\", \"v\": \"冒险\"}, {\"n\": \"枪战\", \"v\": \"枪战\"}, {\"n\": \"恐怖\", \"v\": \"恐怖\"}, {\"n\": \"悬疑\", \"v\": \"悬疑\"}, {\"n\": \"惊悚\", \"v\": \"惊悚\"}, {\"n\": \"经典\", \"v\": \"经典\"}, {\"n\": \"青春\", \"v\": \"青春\"}, {\"n\": \"文艺\", \"v\": \"文艺\"}, {\"n\": \"古装\", \"v\": \"古装\"}, {\"n\": \"历史\", \"v\": \"历史\"}, {\"n\": \"微电影\", \"v\": \"微电影\"}]}, {\"key\": \"1\", \"name\": \"地区\", \"value\": [{\"n\": \"大陆\", \"v\": \"大陆\"}, {\"n\": \"香港\", \"v\": \"香港\"}, {\"n\": \"台湾\", \"v\": \"台湾\"}, {\"n\": \"美国\", \"v\": \"美国\"}, {\"n\": \"法国\", \"v\": \"法国\"}, {\"n\": \"英国\", \"v\": \"英国\"}, {\"n\": \"日本\", \"v\": \"日本\"}, {\"n\": \"韩国\", \"v\": \"韩国\"}, {\"n\": \"德国\", \"v\": \"德国\"}, {\"n\": \"泰国\", \"v\": \"泰国\"}, {\"n\": \"印度\", \"v\": \"印度\"}, {\"n\": \"意大利\", \"v\": \"意大利\"}, {\"n\": \"西班牙\", \"v\": \"西班牙\"}, {\"n\": \"加拿大\", \"v\": \"加拿大\"}, {\"n\": \"其他\", \"v\": \"其他\"}]}, {\"key\": \"11\", \"name\": \"年份\", \"value\": [{\"n\": \"2023\", \"v\": \"2023\"}, {\"n\": \"2022\", \"v\": \"2022\"}, {\"n\": \"2021\", \"v\": \"2021\"}, {\"n\": \"2020\", \"v\": \"2020\"}, {\"n\": \"2019\", \"v\": \"2019\"}, {\"n\": \"2018\", \"v\": \"2018\"}, {\"n\": \"2017\", \"v\": \"2017\"}, {\"n\": \"2016\", \"v\": \"2016\"}, {\"n\": \"2015\", \"v\": \"2015\"}, {\"n\": \"2014\", \"v\": \"2014\"}, {\"n\": \"2013\", \"v\": \"2013\"}, {\"n\": \"2012\", \"v\": \"2012\"}, {\"n\": \"2011\", \"v\": \"2011\"}, {\"n\": \"2010\", \"v\": \"2010\"}]}, {\"key\": \"5\", \"name\": \"字母\", \"value\": [{\"n\": \"A\", \"v\": \"A\"}, {\"n\": \"B\", \"v\": \"B\"}, {\"n\": \"C\", \"v\": \"C\"}, {\"n\": \"D\", \"v\": \"D\"}, {\"n\": \"E\", \"v\": \"E\"}, {\"n\": \"F\", \"v\": \"F\"}, {\"n\": \"G\", \"v\": \"G\"}, {\"n\": \"H\", \"v\": \"H\"}, {\"n\": \"I\", \"v\": \"I\"}, {\"n\": \"J\", \"v\": \"J\"}, {\"n\": \"K\", \"v\": \"K\"}, {\"n\": \"L\", \"v\": \"L\"}, {\"n\": \"M\", \"v\": \"M\"}, {\"n\": \"N\", \"v\": \"N\"}, {\"n\": \"O\", \"v\": \"O\"}, {\"n\": \"P\", \"v\": \"P\"}, {\"n\": \"Q\", \"v\": \"Q\"}, {\"n\": \"R\", \"v\": \"R\"}, {\"n\": \"S\", \"v\": \"S\"}, {\"n\": \"T\", \"v\": \"T\"}, {\"n\": \"U\", \"v\": \"U\"}, {\"n\": \"V\", \"v\": \"V\"}, {\"n\": \"W\", \"v\": \"W\"}, {\"n\": \"X\", \"v\": \"X\"}, {\"n\": \"Y\", \"v\": \"Y\"}, {\"n\": \"Z\", \"v\": \"Z\"}, {\"n\": \"0-9\", \"v\": \"0-9\"}]}, {\"key\": \"2\", \"name\": \"排序\", \"value\": [{\"n\": \"时间\", \"v\": \"time\"}, {\"n\": \"人气\", \"v\": \"hits\"}, {\"n\": \"评分\", \"v\": \"score\"}]}], \"20\": [{\"key\": \"1\", \"name\": \"地区\", \"value\": [{\"n\": \"大陆\", \"v\": \"大陆\"}, {\"n\": \"香港\", \"v\": \"香港\"}, {\"n\": \"台湾\", \"v\": \"台湾\"}, {\"n\": \"美国\", \"v\": \"美国\"}, {\"n\": \"法国\", \"v\": \"法国\"}, {\"n\": \"英国\", \"v\": \"英国\"}, {\"n\": \"日本\", \"v\": \"日本\"}, {\"n\": \"韩国\", \"v\": \"韩国\"}, {\"n\": \"德国\", \"v\": \"德国\"}, {\"n\": \"泰国\", \"v\": \"泰国\"}, {\"n\": \"印度\", \"v\": \"印度\"}, {\"n\": \"意大利\", \"v\": \"意大利\"}, {\"n\": \"西班牙\", \"v\": \"西班牙\"}, {\"n\": \"加拿大\", \"v\": \"加拿大\"}, {\"n\": \"其他\", \"v\": \"其他\"}]}, {\"key\": \"4\", \"name\": \"语言\", \"value\": [{\"n\": \"国语\", \"v\": \"国语\"}, {\"n\": \"英语\", \"v\": \"英语\"}, {\"n\": \"粤语\", \"v\": \"粤语\"}, {\"n\": \"闽南语\", \"v\": \"闽南语\"}, {\"n\": \"韩语\", \"v\": \"韩语\"}, {\"n\": \"日语\", \"v\": \"日语\"}, {\"n\": \"法语\", \"v\": \"法语\"}, {\"n\": \"德语\", \"v\": \"德语\"}, {\"n\": \"其它\", \"v\": \"其它\"}]}, {\"key\": \"11\", \"name\": \"年份\", \"value\": [{\"n\": \"2023\", \"v\": \"2023\"}, {\"n\": \"2022\", \"v\": \"2022\"}, {\"n\": \"2021\", \"v\": \"2021\"}, {\"n\": \"2020\", \"v\": \"2020\"}, {\"n\": \"2019\", \"v\": \"2019\"}, {\"n\": \"2018\", \"v\": \"2018\"}, {\"n\": \"2017\", \"v\": \"2017\"}, {\"n\": \"2016\", \"v\": \"2016\"}, {\"n\": \"2015\", \"v\": \"2015\"}, {\"n\": \"2014\", \"v\": \"2014\"}, {\"n\": \"2013\", \"v\": \"2013\"}, {\"n\": \"2012\", \"v\": \"2012\"}, {\"n\": \"2011\", \"v\": \"2011\"}, {\"n\": \"2010\", \"v\": \"2010\"}]}, {\"key\": \"5\", \"name\": \"字母\", \"value\": [{\"n\": \"A\", \"v\": \"A\"}, {\"n\": \"B\", \"v\": \"B\"}, {\"n\": \"C\", \"v\": \"C\"}, {\"n\": \"D\", \"v\": \"D\"}, {\"n\": \"E\", \"v\": \"E\"}, {\"n\": \"F\", \"v\": \"F\"}, {\"n\": \"G\", \"v\": \"G\"}, {\"n\": \"H\", \"v\": \"H\"}, {\"n\": \"I\", \"v\": \"I\"}, {\"n\": \"J\", \"v\": \"J\"}, {\"n\": \"K\", \"v\": \"K\"}, {\"n\": \"L\", \"v\": \"L\"}, {\"n\": \"M\", \"v\": \"M\"}, {\"n\": \"N\", \"v\": \"N\"}, {\"n\": \"O\", \"v\": \"O\"}, {\"n\": \"P\", \"v\": \"P\"}, {\"n\": \"Q\", \"v\": \"Q\"}, {\"n\": \"R\", \"v\": \"R\"}, {\"n\": \"S\", \"v\": \"S\"}, {\"n\": \"T\", \"v\": \"T\"}, {\"n\": \"U\", \"v\": \"U\"}, {\"n\": \"V\", \"v\": \"V\"}, {\"n\": \"W\", \"v\": \"W\"}, {\"n\": \"X\", \"v\": \"X\"}, {\"n\": \"Y\", \"v\": \"Y\"}, {\"n\": \"Z\", \"v\": \"Z\"}, {\"n\": \"0-9\", \"v\": \"0-9\"}]}, {\"key\": \"2\", \"name\": \"排序\", \"value\": [{\"n\": \"时间\", \"v\": \"time\"}, {\"n\": \"人气\", \"v\": \"hits\"}, {\"n\": \"评分\", \"v\": \"score\"}]}], \"24\": [{\"key\": \"1\", \"name\": \"地区\", \"value\": [{\"n\": \"大陆\", \"v\": \"大陆\"}, {\"n\": \"香港\", \"v\": \"香港\"}, {\"n\": \"台湾\", \"v\": \"台湾\"}, {\"n\": \"美国\", \"v\": \"美国\"}, {\"n\": \"法国\", \"v\": \"法国\"}, {\"n\": \"英国\", \"v\": \"英国\"}, {\"n\": \"日本\", \"v\": \"日本\"}, {\"n\": \"韩国\", \"v\": \"韩国\"}, {\"n\": \"德国\", \"v\": \"德国\"}, {\"n\": \"泰国\", \"v\": \"泰国\"}, {\"n\": \"印度\", \"v\": \"印度\"}, {\"n\": \"意大利\", \"v\": \"意大利\"}, {\"n\": \"西班牙\", \"v\": \"西班牙\"}, {\"n\": \"加拿大\", \"v\": \"加拿大\"}, {\"n\": \"其他\", \"v\": \"其他\"}]}, {\"key\": \"4\", \"name\": \"语言\", \"value\": [{\"n\": \"国语\", \"v\": \"国语\"}, {\"n\": \"英语\", \"v\": \"英语\"}, {\"n\": \"粤语\", \"v\": \"粤语\"}, {\"n\": \"闽南语\", \"v\": \"闽南语\"}, {\"n\": \"韩语\", \"v\": \"韩语\"}, {\"n\": \"日语\", \"v\": \"日语\"}, {\"n\": \"法语\", \"v\": \"法语\"}, {\"n\": \"德语\", \"v\": \"德语\"}, {\"n\": \"其它\", \"v\": \"其它\"}]}, {\"key\": \"11\", \"name\": \"年份\", \"value\": [{\"n\": \"2023\", \"v\": \"2023\"}, {\"n\": \"2022\", \"v\": \"2022\"}, {\"n\": \"2021\", \"v\": \"2021\"}, {\"n\": \"2020\", \"v\": \"2020\"}, {\"n\": \"2019\", \"v\": \"2019\"}, {\"n\": \"2018\", \"v\": \"2018\"}, {\"n\": \"2017\", \"v\": \"2017\"}, {\"n\": \"2016\", \"v\": \"2016\"}, {\"n\": \"2015\", \"v\": \"2015\"}, {\"n\": \"2014\", \"v\": \"2014\"}, {\"n\": \"2013\", \"v\": \"2013\"}, {\"n\": \"2012\", \"v\": \"2012\"}, {\"n\": \"2011\", \"v\": \"2011\"}, {\"n\": \"2010\", \"v\": \"2010\"}]}, {\"key\": \"5\", \"name\": \"字母\", \"value\": [{\"n\": \"A\", \"v\": \"A\"}, {\"n\": \"B\", \"v\": \"B\"}, {\"n\": \"C\", \"v\": \"C\"}, {\"n\": \"D\", \"v\": \"D\"}, {\"n\": \"E\", \"v\": \"E\"}, {\"n\": \"F\", \"v\": \"F\"}, {\"n\": \"G\", \"v\": \"G\"}, {\"n\": \"H\", \"v\": \"H\"}, {\"n\": \"I\", \"v\": \"I\"}, {\"n\": \"J\", \"v\": \"J\"}, {\"n\": \"K\", \"v\": \"K\"}, {\"n\": \"L\", \"v\": \"L\"}, {\"n\": \"M\", \"v\": \"M\"}, {\"n\": \"N\", \"v\": \"N\"}, {\"n\": \"O\", \"v\": \"O\"}, {\"n\": \"P\", \"v\": \"P\"}, {\"n\": \"Q\", \"v\": \"Q\"}, {\"n\": \"R\", \"v\": \"R\"}, {\"n\": \"S\", \"v\": \"S\"}, {\"n\": \"T\", \"v\": \"T\"}, {\"n\": \"U\", \"v\": \"U\"}, {\"n\": \"V\", \"v\": \"V\"}, {\"n\": \"W\", \"v\": \"W\"}, {\"n\": \"X\", \"v\": \"X\"}, {\"n\": \"Y\", \"v\": \"Y\"}, {\"n\": \"Z\", \"v\": \"Z\"}, {\"n\": \"0-9\", \"v\": \"0-9\"}]}, {\"key\": \"2\", \"name\": \"排序\", \"value\": [{\"n\": \"时间\", \"v\": \"time\"}, {\"n\": \"人气\", \"v\": \"hits\"}, {\"n\": \"评分\", \"v\": \"score\"}]}], \"28\": [{\"key\": \"1\", \"name\": \"地区\", \"value\": [{\"n\": \"国产\", \"v\": \"国产\"}, {\"n\": \"日韩\", \"v\": \"日韩\"}, {\"n\": \"欧美\", \"v\": \"欧美\"}]}, {\"key\": \"11\", \"name\": \"年份\", \"value\": [{\"n\": \"2023\", \"v\": \"2023\"}, {\"n\": \"2022\", \"v\": \"2022\"}, {\"n\": \"2021\", \"v\": \"2021\"}, {\"n\": \"2020\", \"v\": \"2020\"}, {\"n\": \"2019\", \"v\": \"2019\"}, {\"n\": \"2018\", \"v\": \"2018\"}, {\"n\": \"2017\", \"v\": \"2017\"}, {\"n\": \"2016\", \"v\": \"2016\"}, {\"n\": \"2015\", \"v\": \"2015\"}, {\"n\": \"2014\", \"v\": \"2014\"}, {\"n\": \"2013\", \"v\": \"2013\"}, {\"n\": \"2012\", \"v\": \"2012\"}, {\"n\": \"2011\", \"v\": \"2011\"}, {\"n\": \"2010\", \"v\": \"2010\"}]}, {\"key\": \"5\", \"name\": \"字母\", \"value\": [{\"n\": \"A\", \"v\": \"A\"}, {\"n\": \"B\", \"v\": \"B\"}, {\"n\": \"C\", \"v\": \"C\"}, {\"n\": \"D\", \"v\": \"D\"}, {\"n\": \"E\", \"v\": \"E\"}, {\"n\": \"F\", \"v\": \"F\"}, {\"n\": \"G\", \"v\": \"G\"}, {\"n\": \"H\", \"v\": \"H\"}, {\"n\": \"I\", \"v\": \"I\"}, {\"n\": \"J\", \"v\": \"J\"}, {\"n\": \"K\", \"v\": \"K\"}, {\"n\": \"L\", \"v\": \"L\"}, {\"n\": \"M\", \"v\": \"M\"}, {\"n\": \"N\", \"v\": \"N\"}, {\"n\": \"O\", \"v\": \"O\"}, {\"n\": \"P\", \"v\": \"P\"}, {\"n\": \"Q\", \"v\": \"Q\"}, {\"n\": \"R\", \"v\": \"R\"}, {\"n\": \"S\", \"v\": \"S\"}, {\"n\": \"T\", \"v\": \"T\"}, {\"n\": \"U\", \"v\": \"U\"}, {\"n\": \"V\", \"v\": \"V\"}, {\"n\": \"W\", \"v\": \"W\"}, {\"n\": \"X\", \"v\": \"X\"}, {\"n\": \"Y\", \"v\": \"Y\"}, {\"n\": \"Z\", \"v\": \"Z\"}, {\"n\": \"0-9\", \"v\": \"0-9\"}]}, {\"key\": \"2\", \"name\": \"排序\", \"value\": [{\"n\": \"时间\", \"v\": \"time\"}, {\"n\": \"人气\", \"v\": \"hits\"}, {\"n\": \"评分\", \"v\": \"score\"}]}], \"32\": [{\"key\": \"5\", \"name\": \"字母\", \"value\": [{\"n\": \"A\", \"v\": \"A\"}, {\"n\": \"B\", \"v\": \"B\"}, {\"n\": \"C\", \"v\": \"C\"}, {\"n\": \"D\", \"v\": \"D\"}, {\"n\": \"E\", \"v\": \"E\"}, {\"n\": \"F\", \"v\": \"F\"}, {\"n\": \"G\", \"v\": \"G\"}, {\"n\": \"H\", \"v\": \"H\"}, {\"n\": \"I\", \"v\": \"I\"}, {\"n\": \"J\", \"v\": \"J\"}, {\"n\": \"K\", \"v\": \"K\"}, {\"n\": \"L\", \"v\": \"L\"}, {\"n\": \"M\", \"v\": \"M\"}, {\"n\": \"N\", \"v\": \"N\"}, {\"n\": \"O\", \"v\": \"O\"}, {\"n\": \"P\", \"v\": \"P\"}, {\"n\": \"Q\", \"v\": \"Q\"}, {\"n\": \"R\", \"v\": \"R\"}, {\"n\": \"S\", \"v\": \"S\"}, {\"n\": \"T\", \"v\": \"T\"}, {\"n\": \"U\", \"v\": \"U\"}, {\"n\": \"V\", \"v\": \"V\"}, {\"n\": \"W\", \"v\": \"W\"}, {\"n\": \"X\", \"v\": \"X\"}, {\"n\": \"Y\", \"v\": \"Y\"}, {\"n\": \"Z\", \"v\": \"Z\"}, {\"n\": \"0-9\", \"v\": \"0-9\"}]}, {\"key\": \"2\", \"name\": \"排序\", \"value\": [{\"n\": \"时间\", \"v\": \"time\"}, {\"n\": \"人气\", \"v\": \"hits\"}, {\"n\": \"评分\", \"v\": \"score\"}]}]}");
		} catch (JSONException e) {
			e.printStackTrace();
			SpiderDebug.log(e);
		}
	}
	
	@Override
	public String homeContent(boolean filter) {
		List<Class> classes = new ArrayList<>();
		Document doc = Jsoup.parse(OkHttp.string(siteURL, getHeader()));
		Elements elements = doc.select(".nav-link");
		for (Element e : elements) {
			Matcher mather = regexCategory.matcher(e.attr("href"));
			if (mather.find()) {
				classes.add(new Class(mather.group(1), e.text().trim()));
			}
		}
		return Result.string(classes, this.parseVodListFromDoc(doc), filter ? filters : null);
	}
	
	@Override
	public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
		String[] urlParams = new String[]{tid, "", "", "", "", "", "", "", pg, "", "", ""};
		if (extend != null && extend.size() > 0) {
			for (String key : extend.keySet()) {
				urlParams[Integer.parseInt(key)] = extend.get(key);
			}
		}
		
		Document doc = Jsoup.parse(OkHttp.string(siteURL + String.format("/index.php/vodshow/%s.html", String.join("-", urlParams)), getHeader()));
		int page = Integer.parseInt(pg), limit = 72, total = 0;
		Matcher matcher = regexPageTotal.matcher(doc.html());
		if (matcher.find()) {
			total = Integer.parseInt(matcher.group(1));
		}
		int count = total <= limit ? 1 : ((int) Math.ceil(total / (double) limit));
		return Result.get().vod(this.parseVodListFromDoc(doc)).page(page, count, limit, total).string();
	}
	
	private List<Vod> parseVodListFromDoc(Document doc) {
		List<Vod> list = new ArrayList<>();
		Elements elements = doc.select(".module-item");
		for (Element e : elements) {
			String vodId = e.selectFirst(".video-name a").attr("href");
			String vodPic = e.selectFirst(".module-item-pic > img").attr("data-src");
			String vodName = e.selectFirst(".video-name").text();
			String vodRemarks = e.selectFirst(".module-item-text").text();
			list.add(new Vod(vodId, vodName, vodPic, vodRemarks));
		}
		return list;
	}
	
	@Override
	public String detailContent(List<String> ids) throws Exception {
		Matcher matcher = regexAli.matcher(OkHttp.string(siteURL + ids.get(0), getHeader()));
		if (matcher.find()) return super.detailContent(Collections.singletonList(matcher.group(1)));
		return "";
	}
	
	@Override
	public String searchContent(String key, boolean quick) throws Exception {
		return searchContent(key, "1");
	}
	
	@Override
	public String searchContent(String key, boolean quick, String pg) throws Exception {
		return searchContent(key, pg);
	}
	
	private String searchContent(String key, String pg) {
		String searchURL = siteURL + String.format("/index.php/vodsearch/%s----------%s---.html", URLEncoder.encode(key), pg);
		String html = OkHttp.string(searchURL, getHeader());
		Elements items = Jsoup.parse(html).select(".module-search-item");
		List<Vod> list = new ArrayList<>();
		for (Element item : items) {
			String vodId = item.select(".video-serial").attr("href");
			String name = item.select(".video-serial").attr("title");
			String pic = item.select(".module-item-pic > img").attr("data-src");
			String remark = item.select(".video-tag-icon").text();
			list.add(new Vod(vodId, name, pic, remark));
		}
		return Result.string(list);
	}
}