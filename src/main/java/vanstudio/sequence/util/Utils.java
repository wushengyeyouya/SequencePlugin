package vanstudio.sequence.util;

import com.google.gson.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBuilder;
import com.intellij.ui.jcef.JBCefCookie;
import com.intellij.ui.jcef.JBCefCookieManager;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.cef.browser.CefBrowser;
import vanstudio.sequence.config.SequenceParamsState;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Utils {

    public static final Gson gson = new GsonBuilder().disableHtmlEscaping().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").serializeNulls()
            .registerTypeAdapter(Double.class, (JsonSerializer<Double>) (t, type, jsonSerializationContext) -> {
                if (t == t.longValue()) {
                    return new JsonPrimitive(t.longValue());
                } else {
                    return new JsonPrimitive(t);
                }
            })
            .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
            .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> new Date(json.getAsLong()))
            .create();
    private static final Logger LOGGER = Logger.getInstance(Utils.class);

    public static final String BDP_AGENT_URL = "http://sit.agent.bdp.weoa.com";

    private static volatile JBCefCookieManager cookies = null;
    private static final BasicCookieStore cookieStore = new BasicCookieStore();
    private static final CloseableHttpClient httpClient = HttpClients
            .custom()
            .setDefaultCookieStore(cookieStore)
            .setMaxConnTotal(5)
            .setMaxConnPerRoute(2)
            .build();

    public static JBCefBrowser createJBCefBrowser() {
        // 创建 JBCefBrowser
        JBCefBrowser jbCefBrowser = JBCefBrowser.create(JBCefBrowser.createBuilder());
        if (cookies == null) {
            synchronized (BDP_AGENT_URL) {
                if(cookies == null) {
                    cookies = jbCefBrowser.getJBCefCookieManager();
                }
            }
        }
        jbCefBrowser.setJBCefCookieManager(cookies);
        return jbCefBrowser;
    }

    public static CloseableHttpClient getHttpClient() {
        if (cookies == null) {
            throw new NullPointerException("Please open BDP-Agent window to login at first(请先打开BDP-Agent窗口完成登录！)");
        }
        List<JBCefCookie> cookieList = cookies.getCookies();
        if (cookieList.isEmpty()) {
            throw new NullPointerException("Please login BDP-Agent at first(请先登录BDP-Agent).");
        }
        cookieList.forEach(cookie -> {
            boolean isExist = cookieStore.getCookies().stream()
                    .anyMatch(existed -> existed.getName().equals(cookie.getName()) && existed.getDomain().equals(cookie.getDomain()));
            if (!isExist) {
                BasicClientCookie cookie1 = new BasicClientCookie(cookie.getName(), cookie.getValue());
                cookie1.setDomain(cookie.getDomain());
                cookie1.setPath(cookie.getPath());
                cookieStore.addCookie(cookie1);
                LOGGER.info("add cookie " + cookie1 + " to Host " + cookie1.getDomain());
            }
        });
        return httpClient;
    }

    public static boolean isDevMode() {
        return SequenceParamsState.getInstance().agentUrl.contains("127.0.0.1");
    }

    private static String getUrl(String uri, boolean isBackend) {
        String url = SequenceParamsState.getInstance().agentUrl;
        if (isBackend && url.contains("127.0.0.1")) {
            url = url.replace("8080", "8081");
        }
        if(url.endsWith("/")) {
            return url + uri;
        } else {
            return url + "/" + uri;
        }
    }

    public static String getCreateTaskUrl() {
        return getUrl("api/requirement/create", true);
    }

    public static String getLoginUrl() {
        return getUrl("user_login.html", false);
    }

    public static String getTaskUrl(int taskId) {
        return getUrl("task.html?task_id=" + taskId, false);
    }

    public static String getDevDocGenerationUrl() {
        return getUrl("api/step_design/genDevDoc", true);
    }

    public static String getWriteToRAGUrl() {
        return getUrl("api/step_rag/writeToRAG", true);
    }

    public static void validateAgentResponse(Map<String, Object> responseBody) {
        if (responseBody.containsKey("success")) {
            boolean success = (boolean) responseBody.get("success");
            if (!success) {
                throw new RuntimeException(String.valueOf(responseBody.get("error")));
            }
        } else {
            throw new RuntimeException(gson.toJson(responseBody));
        }
    }

}
