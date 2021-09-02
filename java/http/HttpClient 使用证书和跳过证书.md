# HttpClient 使用证书和跳过证书

引用
compile ‘org.apache.httpcomponents:httpclient:4.5.10’
使用证书

```java
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class ClientSSLPost {

	public static void main(String[] args) {
		CloseableHttpClient httpclient = null;
		try {
			HttpPost httpPost = new HttpPost("https://127.0.0.1:8080/hello?orderid=111");
			InputStream ca = new FileInputStream(new File("E:/temp/key/client.cer"));// 客户端证书
			// 证书的别名，即:key。 注:cAalias只需要保证唯一即可，不过推荐使用生成keystore时使用的别名。
			String cAalias = System.currentTimeMillis()+""+new SecureRandom().nextInt(1000);

			KeyStore keyStore = ClientSSLTest.getKeyStore(ca, cAalias);
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);
			TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
			if (trustManagers.length != 1|| !(trustManagers[0] instanceof X509TrustManager)) {
				throw new IllegalStateException("Unexpected default trust managers:"+ Arrays.toString(trustManagers));
			}
			X509TrustManager x509TrustManager = (X509TrustManager) trustManagers[0];
			// 这里传TLS或SSL其实都可以的
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { x509TrustManager },new SecureRandom());
			//有时因为证书的域名和实现不一样会报错,解决方法 new SSLConnectionSocketFactory(sslContext,NoopHostnameVerifier.INSTANCE);
			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);

			httpclient = HttpClientBuilder.create().setSSLSocketFactory(sslSocketFactory).build();
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
				public String handleResponse(final HttpResponse response)throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >=200  && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity): null;
					} else {
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}
			};
			String responseBody = httpclient.execute(httpPost, responseHandler);
			System.out.println(responseBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
```

跳过证书

```java
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class ClientNoSSLPostTest {

	public static void main(String[] args) {
		HttpPost httpPost = new HttpPost("https://127.0.0.1:8080/hello?orderid=111");
		// 请求头
		httpPost.addHeader("Accept", "application/json");
		httpPost.addHeader("Connection", "keep-alive");
		httpPost.addHeader("Content-Type", "application/json");

		RequestConfig config = RequestConfig.custom().setConnectTimeout(100).setSocketTimeout(80).build();

		// 这部分代码是重点
		SSLContext sslContext = getSSLContext();
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
				.<ConnectionSocketFactory> create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", new SSLConnectionSocketFactory(sslContext))
				.build();
		PoolingHttpClientConnectionManager mananger = new PoolingHttpClientConnectionManager(
				socketFactoryRegistry);
		mananger.setMaxTotal(100);
		mananger.setDefaultMaxPerRoute(20);

		CloseableHttpClient client = HttpClients.custom()
				.setDefaultRequestConfig(config).setConnectionManager(mananger).build();

		try {
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
				public String handleResponse(final HttpResponse response)
						throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity): null;
					} else {
						HttpEntity entity = response.getEntity();
						System.out.println(EntityUtils.toString(entity));
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}
			};
			String responseBody = client.execute(httpPost, responseHandler);
			System.out.println(responseBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static SSLContext getSSLContext() {
		try {
			// 这里可以填两种值 TLS和LLS , 具体差别可以自行搜索
			SSLContext sc = SSLContext.getInstance("TLS");
			// 构建新对象
			X509TrustManager manager = new X509TrustManager() {
				@Override
				public void checkClientTrusted(
						X509Certificate[] x509Certificates, String s)throws CertificateException {
				}
				@Override
				public void checkServerTrusted(
						X509Certificate[] x509Certificates, String s)throws CertificateException {
				}
				// 这里返回Null
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			sc.init(null, new TrustManager[] { manager }, null);
			return sc;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
```