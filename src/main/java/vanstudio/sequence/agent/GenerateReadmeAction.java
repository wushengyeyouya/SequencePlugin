package vanstudio.sequence.agent;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import vanstudio.sequence.SequenceService;
import vanstudio.sequence.ShowSequenceAction;
import vanstudio.sequence.ui.TaskUI;
import vanstudio.sequence.util.Utils;

import javax.swing.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static vanstudio.sequence.agent.CreateFileAgentEventHandler.CREATE_FILE_OPERATION_TYPE;

public class GenerateReadmeAction extends AnAction implements DumbAware {

    private final Logger logger = Logger.getInstance(getClass());

    public GenerateReadmeAction() {
    }

    public GenerateReadmeAction(String title, String description, Icon icon) {
        super(title, description, icon);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        PsiElement psiElement = event.getData(CommonDataKeys.PSI_FILE);
        if(psiElement == null || psiElement.getContainingFile() == null) {
            return;
        }
        String fileName = psiElement.getContainingFile().getName();
        event.getPresentation().setEnabled(fileName.endsWith(".md") || fileName.endsWith(".java") ||
                fileName.endsWith(".scala"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }
        Map<String, Object> paramsMap = new HashMap<>();
        setParams(paramsMap, anActionEvent);
        StringBuilder sb = new StringBuilder();
        paramsMap.forEach((key, value) -> {
            try {
                sb.append(key).append("=").append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.toString())).append("&");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        });
        String url = Utils.getIDEAUrl(sb.substring(0, sb.length() - 1));
        TaskUI taskUI = new TaskUI(project, url);
        logger.info("try to open BDP-Agent url " + url);
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(SequenceService.PLUGIN_NAME);
        if (toolWindow == null) {
            return;
        }
        ContentManager contentManager = toolWindow.getContentManager();
        final Content content = contentManager.getFactory().createContent(taskUI.getMainPanel(), getUiTitle(), true);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }

    protected void setParams(Map<String, Object> paramsMap, AnActionEvent anActionEvent) {
        String projectName = anActionEvent.getProject().getName();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("app_name", projectName);
        paramsMap.put("request_body", Utils.gson.toJson(requestBody));
        paramsMap.put("operationType", CREATE_FILE_OPERATION_TYPE);
        paramsMap.put("path", "README-ZH.md");
        paramsMap.put("overwrite", true);
        paramsMap.put("content_key", "readme");
        paramsMap.put("show_message", getUiTitle());
        paramsMap.put("uri", Utils.getGenerateReadmeUri());
    }

    protected String getUiTitle() {
        return "Generate README";
    }

}
