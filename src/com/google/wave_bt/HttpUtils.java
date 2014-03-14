package com.google.wave_bt;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.*;

public class HttpUtils {

	// 解析uri并返回字符串
	public static String get(String uri) throws ClientProtocolException,
			IOException {
		// ContentEncodingHttpClient对象采取G-ZIP形式处理HTTP连接
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(uri);
		HttpResponse httpResponse = httpClient.execute(httpGet);

		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if (statusCode >= 200 && statusCode < 400) {
			StringBuilder stringBuilder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					httpResponse.getEntity().getContent(), "UTF-8"));// 编码格式设置
			for (String s = reader.readLine(); s != null; s = reader.readLine()) {
				stringBuilder.append(s);
			}
			reader.close();
			System.out.println("HTTP GET:" + uri.toString());
			System.out.println("从URL获取的字符串:" + stringBuilder.toString());
			return stringBuilder.toString();
		} else {
			return "网络连接错误，请重试 Http " + statusCode + " Error";
		}
	}

	public static String post(String uri, String postMsg)
			throws IOException {
		// ContentEncodingHttpClient对象采取G-ZIP形式处理HTTP连接
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(uri);
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setEntity(new ByteArrayEntity(postMsg.getBytes("UTF8")));
		HttpResponse httpResponse = httpClient.execute(httpPost);

		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if (statusCode >= 200 && statusCode < 400) {
			StringBuilder stringBuilder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					httpResponse.getEntity().getContent(), "UTF-8"));// 编码格式设置
			for (String s = reader.readLine(); s != null; s = reader.readLine()) {
				stringBuilder.append(s);
			}
			reader.close();
			System.out.println("HTTP POST:" + uri.toString());
			System.out.println("从URL获取的字符串:" + stringBuilder.toString());
			return stringBuilder.toString();
		} else {
			return "网络连接错误，请重试 Http " + statusCode + " Error";
		}
	}

	public static boolean download(File file, String uri) throws IOException {
		boolean flag = false;
		file.getParentFile().mkdirs();
		File tmpFile = new File(file.getAbsolutePath() + ".tmp");
		InputStream is = null;
		BufferedOutputStream bos = null;
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(uri);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				System.out.println("开始通过URL生成缓存");
				tmpFile.createNewFile();
				is = httpResponse.getEntity().getContent();
				bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
				byte[] buffer = new byte[4096];
				int i = is.read(buffer);
				while (i != -1) {
					bos.write(buffer, 0, i);
					i = is.read(buffer);
				}
				tmpFile.renameTo(file);
				flag = true;
			}
		} catch (IOException ioe) {
			System.out.println("发生I/O错误,图片下载停止");
			ioe.printStackTrace();
			throw ioe;
		} finally {
			if (bos != null) {
				bos.close();
			}
			if (is != null) {
				is.close();
			}
		}
		return flag;
	}
}
