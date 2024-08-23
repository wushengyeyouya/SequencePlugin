package vanstudio.sequence.ui;

import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.util.ExceptionUtil;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.network.CefRequest;
import vanstudio.sequence.agent.AgentEventHandlerFactory;
import vanstudio.sequence.config.SequenceParamsState;
import vanstudio.sequence.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static vanstudio.sequence.util.Utils.createJBCefBrowser;

public class TaskUI {

    private static final Logger LOGGER = Logger.getInstance(TaskUI.class);

    private final JPanel htmlPanelWrapper;
    private JBCefBrowser jbCefBrowser;
    private String url;
    private boolean isTaskUILoaded = false;

    public TaskUI(Project project, String url) {
        this.url = url;
        htmlPanelWrapper = new JPanel(new BorderLayout());
        if (!JBCefApp.isSupported()) {
            htmlPanelWrapper.add(new JLabel("当前环境不支持JCEF", SwingConstants.CENTER));
            return;
        }
        // 创建 JBCefBrowser
        jbCefBrowser = createJBCefBrowser();
        // register Handler
//        jbCefBrowser.getJBCefClient()
//                .addJSDialogHandler(
//                        new JsDialogHandler(),
//                        jbCefBrowser.getCefBrowser());
        this.htmlPanelWrapper.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
        // load URL
        reload();
        register(jbCefBrowser, project);
    }

    public JBCefBrowser getJbCefBrowser() {
        return jbCefBrowser;
    }

    public void reload() {
        if (url == null) {
            jbCefBrowser.loadURL(SequenceParamsState.getInstance().agentUrl);
        } else {
            jbCefBrowser.loadURL(url);
        }
        jbCefBrowser.getCefBrowser().reloadIgnoreCache();
    }

    public void sendEvent(Map<String, Object> eventMap) {
        String eventStr = Utils.gson.toJson(eventMap);
        String javaScript = "receiveEvent('"+eventStr+"')";
        long startTime = System.currentTimeMillis();
        while(!isTaskUILoaded && System.currentTimeMillis() - startTime < 20000) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        if (!isTaskUILoaded) {
            LOGGER.warn("url " + url + "did not loaded after 30s, ignore the event " + eventStr);
            return;
        }
        LOGGER.info(String.format("try to send event to url(%s) with params: %s",
                jbCefBrowser.getCefBrowser().getURL(), eventStr));
        jbCefBrowser.getCefBrowser().executeJavaScript(javaScript, url, 0);
    }

    private void register(JBCefBrowser browser, Project project) {
        JBCefJSQuery query = JBCefJSQuery.create((JBCefBrowserBase) browser);
        query.addHandler((String arg) -> {
            try {
                String responseMsg = AgentEventHandlerFactory.handle(arg, project);
                return new JBCefJSQuery.Response(responseMsg);
            } catch (JsonSyntaxException e) {
                LOGGER.warn(e);
                return new JBCefJSQuery.Response(ExceptionUtil.getThrowableText(e), 1000, ExceptionUtil.getMessage(e));
            } catch (Exception e) {
                LOGGER.warn(e);
                return new JBCefJSQuery.Response(ExceptionUtil.getThrowableText(e), 2000, ExceptionUtil.getMessage(e));
            }
        });

        browser.getJBCefClient().addLoadHandler(new CefLoadHandler() {
            @Override
            public void onLoadingStateChange(CefBrowser cefBrowser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
            }
            @Override
            public void onLoadStart(CefBrowser cefBrowser, CefFrame frame, CefRequest.TransitionType transitionType) {
            }
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                LOGGER.info("register window.callIDEA.");
                cefBrowser.executeJavaScript("window.callIDEA = function(arg, successCallback, failureCallback) {" +
                        query.inject(
                                "arg",
                                "function(response) {successCallback(response)}",
                                "function(error_code, error_message) {failureCallback(error_code, error_message)}"
                        ) +
                        "};", browser.getCefBrowser().getURL(), 0);
                isTaskUILoaded = true;
            }

            @Override
            public void onLoadError(CefBrowser cefBrowser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                LOGGER.warn("load error. errorCode: " + errorCode + ", errorText: " + errorText);
            }
        }, browser.getCefBrowser());
    }

    public JPanel getMainPanel() {
        return htmlPanelWrapper;
    }
}