package vanstudio.sequence.ui;

import com.google.gson.JsonSyntaxException;
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

import javax.swing.*;
import java.awt.*;

import static vanstudio.sequence.util.Utils.createJBCefBrowser;

public class TaskUI {

    private final JPanel htmlPanelWrapper;
    private JBCefBrowser jbCefBrowser;
    private String url;

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

    public void reload() {
        if (url == null) {
            jbCefBrowser.loadURL(SequenceParamsState.getInstance().agentUrl);
        } else {
            jbCefBrowser.loadURL(url);
        }
    }

    private void register(JBCefBrowser browser, Project project) {
        JBCefJSQuery query = JBCefJSQuery.create((JBCefBrowserBase) browser);
        query.addHandler((String arg) -> {
            try {
                String responseMsg = AgentEventHandlerFactory.handle(arg, project);
                return new JBCefJSQuery.Response(responseMsg);
            } catch (JsonSyntaxException e) {
                return new JBCefJSQuery.Response(ExceptionUtil.getThrowableText(e), 1000, ExceptionUtil.getMessage(e));
            } catch (Exception e) {
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
                cefBrowser.executeJavaScript(
                        "window.callIDEA = function(arg, successCallback, failureCallback) {" +
                                query.inject(
                                        "arg",
                                        "response => successCallback(response)",
                                        "(error_code, error_message) => failureCallback(error_code, error_message)"
                                ) +
                                "};",
                        null, 0);
            }

            @Override
            public void onLoadError(CefBrowser cefBrowser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
            }
        }, browser.getCefBrowser());
    }

    public JPanel getMainPanel() {
        return htmlPanelWrapper;
    }
}