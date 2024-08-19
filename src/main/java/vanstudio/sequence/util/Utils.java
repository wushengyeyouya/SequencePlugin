package vanstudio.sequence.util;

import com.google.gson.Gson;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBuilder;
import com.intellij.ui.jcef.JBCefCookie;
import com.intellij.ui.jcef.JBCefCookieManager;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import vanstudio.sequence.config.SequenceParamsState;

import java.util.List;
import java.util.Map;

public class Utils {

    public static final Gson gson = new Gson();

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
        JBCefBrowserBuilder builder = JBCefBrowser.createBuilder();
        JBCefBrowser jbCefBrowser = JBCefBrowser.create(builder);
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
        cookieList.forEach(cookie -> {
            cookieStore.addCookie(new BasicClientCookie(cookie.getCefCookie().name, cookie.getCefCookie().value));
        });
        return httpClient;
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
