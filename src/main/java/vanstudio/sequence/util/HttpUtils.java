package vanstudio.sequence.util;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.util.Map;

public class HttpUtils {

    private static final Logger LOGGER = Logger.getInstance(HttpUtils.class);
    private static final RequestConfig requestConfig = getRequestConfig();

    private static RequestConfig getRequestConfig() {
        return RequestConfig.custom().setConnectTimeout(60000)
                .setConnectionRequestTimeout(60000)
                .setSocketTimeout(25 * 60 * 1000).build();
    }

    public static Map<String, Object> get(String url,
                          Map<String, String> headers,
                          Map<String, Object> params) throws Exception {
        LOGGER.info(String.format("send get to '%s' with params: %s, headers: %s.", url, params, headers));
        URIBuilder builder = new URIBuilder(url);
        if (MapUtils.isNotEmpty(params)) {
            params.forEach((key, value) -> {
                if(key != null && value != null) {
                    builder.addParameter(key, value.toString());
                }
            });
        }
        HttpGet httpGet = new HttpGet(builder.build());
        httpGet.setConfig(requestConfig);
        if (MapUtils.isNotEmpty(headers)) {
            headers.forEach(httpGet::addHeader);
        }
        // 发送请求并获取响应
        try (CloseableHttpResponse response = Utils.getHttpClient().execute(httpGet)) {
            // 获取响应状态码
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            if (statusCode != 200) {
                LOGGER.warn(String.format("send get to '%s' failed. responseBody: %s", url, responseBody));
                throw new HttpResponseException(statusCode, responseBody);
            }
            return Utils.gson.fromJson(responseBody, Map.class);
        }
    }

    public static Map<String, Object> post(String url,
                                           Map<String, String> headers,
                                           String requestBody) throws Exception {
        LOGGER.info(String.format("send post to '%s' with requestBody: %s, headers: %s.", url, requestBody, headers));
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);
        if (StringUtils.isNotEmpty(requestBody)) {
            StringEntity stringEntity = new StringEntity(requestBody, "UTF-8");
            stringEntity.setContentEncoding("UTF-8");
            stringEntity.setContentType("application/json");
            httpPost.setEntity(stringEntity);
        }
        if (MapUtils.isNotEmpty(headers)) {
            headers.forEach(httpPost::addHeader);
        }
        // 发送请求并获取响应
        try (CloseableHttpResponse response = Utils.getHttpClient().execute(httpPost)) {
            // 获取响应状态码
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            if (statusCode != 200) {
                LOGGER.warn(String.format("send post to '%s' failed. responseBody: %s", url, responseBody));
                throw new HttpResponseException(statusCode, responseBody);
            }
            return Utils.gson.fromJson(responseBody, Map.class);
        }
    }

    public static Map<String, Object> post(String url,
                                          Map<String, String> headers,
                                          Map<String, Object> requestBodies) throws Exception {
        if(MapUtils.isNotEmpty(requestBodies)) {
            return post(url, headers, Utils.gson.toJson(requestBodies));
        }
        return post(url, headers, "");
    }
}
