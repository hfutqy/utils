```
import java.io.ByteArrayInputStream;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.lz.common.util.http.FileItem;

public class HttpClientUtil {

	private static Logger logger = LoggerFactory
			.getLogger(HttpClientUtil.class);

	public static void main(String[] args) {
		HttpURLConnection conn = null;
		InputStream ins = null;
		try {
//			String boundary = System.currentTimeMillis() + "";
//			String ctype = "multipart/form-data;boundary=" + boundary;
//			conn = HttpClientUtil
//					.getConnection(
//							null,
//							new URL(
//									"http://10.75.201.68:8888/cfile/file/image?imageId=group2/M00/05/64/CkvJo1cQVPyATVbKACIyO0-AKoo7735712"),
//							"GET", ctype, null);
//			conn.setConnectTimeout(1000);
//			conn.setReadTimeout(5000);
			byte[] bytes = downLoadFile("http://10.75.201.68:8888/cfile/file/image?imageId=group2/M00/05/64/CkvJo1cQVPyATVbKACIyO0-AKoo7735712");
            InputStream inputStream =new  ByteArrayInputStream(bytes);
			// ins = conn.getInputStream();
			File file = new File("D:/6.docx");
			FileOutputStream fot = new FileOutputStream(file);
			com.slob.util.io.IOUtil.inputStreamToOutputStream(inputStream, fot);
		} catch (Exception e) {

		}
	}
	
	public static byte[] downLoadFile(String url) throws IOException {
		HttpURLConnection conn = null;
		InputStream ins = null;
		byte[] bytes  = null;
		try {
			String boundary = System.currentTimeMillis() + "";
			String ctype = "multipart/form-data;boundary=" + boundary;
			conn = HttpClientUtil.getConnection(null,new URL(url),"GET", ctype, null);
			conn.setConnectTimeout(1000);
			conn.setReadTimeout(5000);
			ins = conn.getInputStream();
			bytes = readBytes(ins);
			return bytes;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (ins != null) {
				ins.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	public static String uploadFile(String url, String fieldName,
			String fileName, InputStream ips, ResponseProcess respProcess)
			throws IOException {

		HttpURLConnection conn = null;
		OutputStream out = null;
		try {
			String boundary = System.currentTimeMillis() + "";
			String ctype = "multipart/form-data;boundary=" + boundary;
			conn = HttpClientUtil.getConnection(null, new URL(url), "POST",
					ctype, null);
			conn.setConnectTimeout(1000);
			conn.setReadTimeout(5000);
			out = conn.getOutputStream();

			byte[] entryBoundaryBytes = ("\r\n--" + boundary + "\r\n")
					.getBytes("UTF-8");
			out.write(entryBoundaryBytes);

			byte[] data = new byte[1024 * 1024];
			int size = ips.read(data);
			byte[] fileBytes = getFileEntry(fieldName, fileName,
					getMimeType(data), "UTF-8");
			out.write(fileBytes);

			while (size > 0) {
				out.write(data, 0, size);
				size = ips.read(data);
			}
			byte[] endBoundaryBytes = ("\r\n--" + boundary + "--\r\n")
					.getBytes("UTF-8");
			out.write(endBoundaryBytes);

			return respProcess.processResponse(conn);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (out != null) {
				out.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	// public static String doPost(Proxy proxy, String url, String requestType,
	// Map<String, String> params,
	// Map<String, FileItem> fileParams, String charset, int connectTimeout, int
	// readTimeout,
	// Map<String, String> headerMap, ResponseProcess respProcess) throws
	// IOException {
	//
	// String boundary = System.currentTimeMillis() + "";
	// HttpURLConnection conn = null;
	// OutputStream out = null;
	// String rsp = null;
	// try {
	// try {
	// String ctype = "multipart/form-data;boundary=" + boundary;
	// conn = getConnection(proxy, new URL(url), requestType, ctype, headerMap);
	// conn.setConnectTimeout(connectTimeout);
	// conn.setReadTimeout(readTimeout);
	// }
	// catch (IOException e) {
	// logger.error(url, e);
	// throw e;
	// }
	//
	// try {
	// out = conn.getOutputStream();
	//
	// byte[] entryBoundaryBytes = ("\r\n--" + boundary +
	// "\r\n").getBytes(charset);
	//
	// if (params != null) {
	// // 文本
	// Set<Entry<String, String>> textEntrySet = params.entrySet();
	// for (Entry<String, String> textEntry : textEntrySet) {
	// byte[] textBytes = getTextEntry(textEntry.getKey(), textEntry.getValue(),
	// charset);
	// out.write(entryBoundaryBytes);
	// out.write(textBytes);
	// }
	// }
	//
	// // 文件
	// if (fileParams != null) {
	// Set<Entry<String, FileItem>> fileEntrySet = fileParams.entrySet();
	// for (Entry<String, FileItem> fileEntry : fileEntrySet) {
	// FileItem fileItem = fileEntry.getValue();
	// if (fileItem.getContent() == null) {
	// continue;
	// }
	// byte[] fileBytes = getFileEntry(fileEntry.getKey(),
	// fileItem.getFileName(),
	// fileItem.getMimeType(), charset);
	// out.write(entryBoundaryBytes);
	// out.write(fileBytes);
	// out.write(fileItem.getContent());
	// }
	// }
	//
	// byte[] endBoundaryBytes = ("\r\n--" + boundary +
	// "--\r\n").getBytes(charset);
	// out.write(endBoundaryBytes);
	// rsp = respProcess.processResponse(conn);
	// }
	// catch (IOException e) {
	// logger.error(url, e);
	// throw e;
	// }
	// }
	// finally {
	// if (out != null) {
	// out.close();
	// }
	// if (conn != null) {
	// conn.disconnect();
	// }
	// }
	//
	// return rsp;
	// }

	public static String doGet(Proxy proxy, String url, String charset,
			ResponseProcess respProcess) throws IOException {

		if (url == null) {
			return "";
		}
		HttpURLConnection conn = null;
		String rsp = null;
		try {
			String ctype = "application/x-www-form-urlencoded;charset="
					+ charset;
			conn = getConnection(proxy, new URL(url), "GET", ctype, null);
			rsp = respProcess.processResponse(conn);
		} catch (IOException e) {
			logger.error(url, e);
			throw e;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return rsp;
	}

	private static class DefaultTrustManager implements X509TrustManager {

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}
	}

	public static HttpURLConnection getConnection(Proxy proxy, URL url,
			String method, String ctype, Map<String, String> headerMap)
			throws IOException {
		HttpURLConnection conn = null;
		if ("https".equals(url.getProtocol())) {
			SSLContext ctx = null;
			try {
				ctx = SSLContext.getInstance("TLS");
				ctx.init(new KeyManager[0],
						new TrustManager[] { new DefaultTrustManager() },
						new SecureRandom());
			} catch (Exception e) {
				throw new IOException(e);
			}
			HttpsURLConnection connHttps;
			if (proxy != null) {
				connHttps = (HttpsURLConnection) url.openConnection(proxy);
			} else {
				connHttps = (HttpsURLConnection) url.openConnection();
			}
			connHttps.setSSLSocketFactory(ctx.getSocketFactory());
			connHttps.setHostnameVerifier(new HostnameVerifier() {

				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
			conn = connHttps;
		} else {
			if (proxy != null) {
				conn = (HttpURLConnection) url.openConnection(proxy);
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}
		}

		conn.setRequestMethod(method);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Accept",
				"text/xml,text/javascript,text/html,application/json");
		conn.setRequestProperty("User-Agent", "java");
		conn.setRequestProperty("Content-Type", ctype);
		if (headerMap != null) {
			for (Map.Entry<String, String> entry : headerMap.entrySet()) {
				conn.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}
		return conn;
	}

	private static byte[] getTextEntry(String fieldName, String fieldValue,
			String charset) throws IOException {
		StringBuilder entry = new StringBuilder();
		entry.append("Content-Disposition:form-data;name=\"");
		entry.append(fieldName);
		entry.append("\"\r\nContent-Type:text/plain\r\n\r\n");
		entry.append(fieldValue);
		return entry.toString().getBytes(charset);
	}

	private static byte[] getFileEntry(String fieldName, String fileName,
			String mimeType, String charset) throws IOException {
		StringBuilder entry = new StringBuilder();
		entry.append("Content-Disposition:form-data;name=\"");
		entry.append(fieldName);
		entry.append("\";filename=\"");
		entry.append(fileName);
		entry.append("\"\r\nContent-Type:");
		entry.append(mimeType);
		entry.append("\r\n\r\n");
		return entry.toString().getBytes(charset);
	}

	public static interface ResponseProcess {

		String processResponse(HttpURLConnection conn);
	};

	public static String getMimeType(byte[] bytes) {
		String suffix = getFileSuffix(bytes);
		String mimeType;

		if ("JPG".equals(suffix)) {
			mimeType = "image/jpeg";
		} else if ("GIF".equals(suffix)) {
			mimeType = "image/gif";
		} else if ("PNG".equals(suffix)) {
			mimeType = "image/png";
		} else if ("BMP".equals(suffix)) {
			mimeType = "image/bmp";
		} else {
			mimeType = "application/octet-stream";
		}

		return mimeType;
	}

	/**
	 * 获取文件的真实后缀名。目前只支持JPG, GIF, PNG, BMP四种图片文件。
	 * 
	 * @param bytes
	 *            文件字节流
	 * @return JPG, GIF, PNG or null
	 */
	public static String getFileSuffix(byte[] bytes) {
		if (bytes == null || bytes.length < 10) {
			return null;
		}

		if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
			return "GIF";
		} else if (bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
			return "PNG";
		} else if (bytes[6] == 'J' && bytes[7] == 'F' && bytes[8] == 'I'
				&& bytes[9] == 'F') {
			return "JPG";
		} else if (bytes[0] == 'B' && bytes[1] == 'M') {
			return "BMP";
		} else {
			return null;
		}
	}
	
	public static byte[] readBytes(InputStream in) throws IOException {  
        byte[] temp = new byte[in.available()];  
        byte[] result = new byte[0];  
        int size = 0;  
        while ((size = in.read(temp)) != -1) {  
            byte[] readBytes = new byte[size];  
            System.arraycopy(temp, 0, readBytes, 0, size);  
            result = mergeArray(result,readBytes);  
        }  
        return result;  
    }  
	
	public static byte[] mergeArray(byte[]... a) {  
        // 合并完之后数组的总长度  
        int index = 0;  
        int sum = 0;  
        for (int i = 0; i < a.length; i++) {  
            sum = sum + a[i].length;  
        }  
        byte[] result = new byte[sum];  
        for (int i = 0; i < a.length; i++) {  
            int lengthOne = a[i].length;  
            if(lengthOne==0){  
                continue;  
            }  
            // 拷贝数组  
            System.arraycopy(a[i], 0, result, index, lengthOne);  
            index = index + lengthOne;  
        }  
        return result;  
    }  
 

}
```
