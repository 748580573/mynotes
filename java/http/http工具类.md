Http工具类

```java
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * http请求工具类
 * Created by zhangwen on 2016/12/26.
 */
public class HttpTools {

    /**
     * HttpStatus
     */
    public static final int SC_ERROR = 0;

    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url   发送请求的URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) {
        StringBuffer result = new StringBuffer("");
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            // String urlNameString =
            // "http://10.0.2.38:8080/miner/public/model/onlineModel/publish.do?secretkey=2ef261d327&publishModelId=b47861d614514d2e9c824d3b8c98175e&body={%20%22datas%22:%20[%20{%20%22Marital_Status%22:%201,%20%22Gender%22:%201,%20%22Weight_Category%22:%201,%20%22Cholesterol%22:%201,%20%22Stress_Management%22:%201,%20%22Trait_Anxiety%22:%201,%20%222nd_Heart_Attack%22:%201%20}%20],%20%22cols%22:%20[%20{%20%22name%22:%20%22Marital_Status%22,%20%22type%22:%20%22double%22%20,%22idCol%22:%20true%20},%20{%20%22name%22:%20%22Gender%22,%20%22type%22:%20%22double%22%20},%20{%20%22name%22:%20%22Weight_Category%22,%20%22type%22:%20%22double%22%20},%20{%20%22name%22:%20%22Cholesterol%22,%20%22type%22:%20%22double%22%20},%20{%20%22name%22:%20%22Stress_Management%22,%20%22type%22:%20%22double%22%20},%20{%20%22name%22:%20%22Trait_Anxiety%22,%20%22type%22:%20%22double%22%20},%20{%20%22name%22:%20%222nd_Heart_Attack%22,%20%22type%22:%20%22double%22%20}%20]%20}";
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // connection.addRequestProperty();
            connection.addRequestProperty("cookie",
                    "org.springframework.web.servlet.i18n.CookieLocaleResolver.LOCALE=en_US");

            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
//            Map<String, List<String>> map = connection.getHeaderFields();
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e.getMessage());
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result.toString();
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url   发送请求的 URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuffer result = new StringBuffer();
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setRequestProperty("Cookie", "org.springframework.web.servlet.i18n.CookieLocaleResolver.LOCALE=zh");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), "utf-8"));
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result.toString();
    }

    public static HttpSession getSession() {
        HttpSession session = null;
        try {
            session = getRequest().getSession();
        } catch (Exception e) {
        }
        return session;
    }

    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs.getRequest();
    }

    private static KeyStore createKeyStore(final URL url, final String password)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if (url == null) {
            throw new IllegalArgumentException("Keystore url may not be null");
        }
        KeyStore keystore = KeyStore.getInstance("jks");
        InputStream is = null;
        try {
            is = url.openStream();
            keystore.load(is, password != null ? password.toCharArray() : null);
        } finally {
            if (is != null)
                is.close();
        }
        return keystore;
    }

    private static KeyManager[] createKeyManagers(final KeyStore keystore, final String password)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(keystore, password != null ? password.toCharArray() : null);
        return kmfactory.getKeyManagers();
    }

    private static TrustManager[] createTrustManagers(final KeyStore keystore)
            throws KeyStoreException, NoSuchAlgorithmException {
        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }
        TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init(keystore);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers();
        return trustmanagers;
    }



    public static String sendPost(String url, Map<String, String> paramMap) {
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuffer result = new StringBuffer("");
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // conn.setRequestProperty("Charset", Constants.ENCODE);
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), "utf-8"));

            // 设置请求属性
            StringBuffer param = new StringBuffer("");
            if (paramMap != null && paramMap.size() > 0) {
                int i = 0;
                for (Map.Entry<String, String> entry:paramMap.entrySet()){
                    i++ ;
                    param.append(entry.getKey());
                    param.append("=");
                    param.append(entry.getValue());
                    if (i != paramMap.size()){
                        param.append("&");
                    }
                }
            }

            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result.toString();
    }


    /**
     * 发送JSON请求
     * @param url
     * @param data
     * @return
     */
    public static String sendJson(String url, String data){
        try {
            URL request = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) request.openConnection();
            PrintWriter out;
            conn.setRequestMethod("POST");
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("charset", "UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            out = new PrintWriter(conn.getOutputStream());
            out.print(data);
            out.flush();
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String str;
            List<String> result = new ArrayList<>();

            while ((str = br.readLine()) != null) {
                result.add(str);
            }
            //关闭流
            is.close();
            conn.disconnect();
            return result.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
```