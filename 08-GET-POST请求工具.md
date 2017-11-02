```
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientUtil {

	static CookieStore cookieStore = null;

	private static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

	public static void main(String[] args) {

		Map<String, String> map = new HashMap<String, String>();
		map.put("pageIndex", "1");
		map.put("pageSize", "20");
		String result = HttpClientUtil.post("http://xxxxx/xxx/operator/menu/findByPage.json", map);
		logger.info("post result:" + result);

		map = new HashMap<String, String>();
		map.put("pageNumber", "1");
		map.put("pageSize", "20");
		result = HttpClientUtil.get("http://xxx/ftc-ump-mid/xxx/dict/condition.json", map);
		logger.info("get result:" + result);

	}

	@Test
	public void postTest() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("pageIndex", "1");
		map.put("pageSize", "20");
		String result = HttpClientUtil.post("http:/xxxxx/findByPage.json", map);
		logger.info("result:" + result);
	}

	@Test
	public void getTest() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("pageNumber", "1");
		map.put("pageSize", "20");
		String result = HttpClientUtil.get("http://xxxxor/dict/condition.json", map);
		logger.info("result:" + result);
	}

	/**
	 * 获取cookie的内容
	 * 
	 * @param ck
	 * @param name
	 * @return
	 */
	public static String retriveCkValue(String ck, String name) {
		if (StringUtils.isBlank(ck) || StringUtils.isBlank(name)) {
			return "";
		}

		final String delimChar = name + "=";
		int delimBegin = ck.indexOf(delimChar);
		if (delimBegin < 0) {
			return "";
		}

		String val = null;
		int delimEnd = ck.indexOf(';', delimBegin);
		if (delimEnd < 0) {
			val = ck.substring(delimBegin + delimChar.length()).trim();
		} else {
			val = ck.substring(delimBegin + delimChar.length(), delimEnd).trim();
		}
		int idx = val.indexOf('?');
		if (idx > 0) {
			val = val.substring(0, idx);
		}

		return val;
	}

	/**
	 * 将cookie保存到静态变量中供后续调用
	 * 
	 * @param httpResponse
	 */
	public static void setCookieStore(HttpResponse httpResponse) {
		logger.info("-------setCookieStore---------");

		if (httpResponse.getFirstHeader("Set-Cookie") != null) {
			cookieStore = new BasicCookieStore();
			org.apache.http.Header[] cookies = httpResponse.getHeaders("Set-Cookie");
			// Expires=Fri, 14-Apr-2017 09:42:26 GMT;

			for (int j = 0; j < cookies.length; j++) {
				String content = cookies[j].getValue();
				String cookName = content.substring(0, content.indexOf("="));

				String cookNameContent = retriveCkValue(content, cookName);
				String domain = retriveCkValue(content, "Domain");
				String path = retriveCkValue(content, "Path");

				String time = retriveCkValue(content, "Expires");
				Date expires = new Date(time);

				BasicClientCookie cookie = new BasicClientCookie(cookName, cookNameContent);
				cookie.setDomain(domain);
				cookie.setPath(path);
				cookie.setExpiryDate(expires);

				cookieStore.addCookie(cookie);
				logger.info(cookName + ":{},domain:{},path:{},expires", cookNameContent, domain, path, expires);

			}
		}
	}

	/**
	 * 模拟登陆
	 * 
	 * @param client
	 * @return
	 */
	private static String login(CloseableHttpClient client) {
		String path = "http://xxxxxx/operator.json";
		Map<String, String> params = new HashMap<String, String>();
		params.put("userId", "ITADMIN2");
		params.put("password", "123456");
		List<NameValuePair> list = new ArrayList<NameValuePair>();
		if (params != null && !params.isEmpty()) {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}
		}
		HttpResponse httpResponse = null;
		try {
			// 实现将请求的参数封装到表单中，即请求体中
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, Consts.UTF_8);
			// 使用post方式提交数据
			HttpPost httpPost = new HttpPost(path);
			int connectionTimeout = 15000;
			int soTimeout = 15000;
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeout).setSocketTimeout(soTimeout).build();
			httpPost.setConfig(requestConfig);
			httpPost.setEntity(entity);

			httpResponse = client.execute(httpPost);
			// 获取服务器端返回的状态码和输入流，将输入流转换成字符串
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				// setCookieStore(httpResponse); // 设置cookie
				return EntityUtils.toString(httpResponse.getEntity(), Consts.UTF_8);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (httpResponse != null) {
				try {
					EntityUtils.consume(httpResponse.getEntity());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return "";
	}

	/**
	 * 模拟get
	 * 
	 * @param path
	 * @param params
	 * @param encode
	 * @return
	 */
	public static String get(String path, Map<String, String> params) {
		List<NameValuePair> list = new ArrayList<NameValuePair>();
		if (params != null && !params.isEmpty()) {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}
		}
		HttpResponse httpResponse = null;
		CloseableHttpClient client = null;
		try {
			// 实现将请求的参数封装到表单中，即请求体中
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, Consts.UTF_8);
			// 转换为键值对
			String str = EntityUtils.toString(entity);

			// 使用get方式提交数据
			HttpGet httpGet = new HttpGet(path + "?" + str);
			int connectionTimeout = 15000;
			int soTimeout = 15000;
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeout).setSocketTimeout(soTimeout).build();
			httpGet.setConfig(requestConfig);
			// 执行get请求，并获取服务器端的响应HttpResponse
			client = HttpClients.createDefault();

			// if (cookieStore != null) {
			// client =
			// HttpClients.custom().setDefaultCookieStore(cookieStore).build();
			// } else {
			// client = HttpClients.createDefault();
			// login(client);
			// }
			login(client);

			httpResponse = client.execute(httpGet);
			// 获取服务器端返回的状态码和输入流，将输入流转换成字符串
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				return EntityUtils.toString(httpResponse.getEntity(), Consts.UTF_8);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (httpResponse != null) {
				try {
					EntityUtils.consume(httpResponse.getEntity());
				} catch (IOException e) {
					logger.error("", e);
				}
			}
			if (client != null) {
				try {
					client.close();
				} catch (IOException e) {
					logger.error("", e);
				}
			}
		}

		return "";
	}

	/**
	 * 模拟post
	 * 
	 * @param path
	 * @param params
	 * @param encode
	 * @return
	 */
	public static String post(String path, Map<String, String> params) {
		List<NameValuePair> list = new ArrayList<NameValuePair>();
		if (params != null && !params.isEmpty()) {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}
		}
		HttpResponse httpResponse = null;
		CloseableHttpClient client = null;
		try {
			// 实现将请求的参数封装到表单中，即请求体中
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, "utf-8");
			// 使用post方式提交数据
			HttpPost httpPost = new HttpPost(path);
			int connectionTimeout = 15000;
			int soTimeout = 15000;
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeout).setSocketTimeout(soTimeout).build();
			httpPost.setConfig(requestConfig);
			httpPost.setEntity(entity);
			// 执行post请求，并获取服务器端的响应HttpResponse
			HttpClients.createDefault();
			login(client);

			// if (cookieStore != null) {
			// client =
			// HttpClients.custom().setDefaultCookieStore(cookieStore).build();
			// } else {
			// client = HttpClients.createDefault();
			// login(client);
			// }

			httpResponse = client.execute(httpPost);
			// 获取服务器端返回的状态码和输入流，将输入流转换成字符串
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				return EntityUtils.toString(httpResponse.getEntity(), "utf-8");
			}

		} catch (Exception e) {
			logger.error("", e);
		} finally {
			if (httpResponse != null) {
				try {
					EntityUtils.consume(httpResponse.getEntity());
				} catch (IOException e) {
					logger.error("", e);
				}
			}

			if (client != null) {
				try {
					client.close();
				} catch (IOException e) {
					logger.error("", e);
				}
			}
		}

		return "";
	}
}
```
