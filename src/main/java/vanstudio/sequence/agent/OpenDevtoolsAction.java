package vanstudio.sequence.agent;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.NotNull;
import vanstudio.sequence.SequenceService;
import vanstudio.sequence.ui.TaskUI;

import javax.swing.*;
import java.awt.*;

public class OpenDevtoolsAction extends AnAction {

    private TaskUI taskUI;

    public OpenDevtoolsAction(TaskUI taskUI) {
        super("Open DevTools", "Open dev tools of .", AllIcons.General.Settings);
        this.taskUI = taskUI;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(SequenceService.PLUGIN_NAME);
        if (toolWindow == null) {
            return;
        }
        CefBrowser devTools = taskUI.getJbCefBrowser().getCefBrowser().getDevTools();
        JBCefBrowser devToolsBrowser = JBCefBrowser.createBuilder()
                .setCefBrowser(devTools)
                .setClient(taskUI.getJbCefBrowser().getJBCefClient())
                .build();
        ContentManager contentManager = toolWindow.getContentManager();
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(devToolsBrowser.getComponent(), BorderLayout.CENTER);
        final Content content = contentManager.getFactory().createContent(mainPanel, "Dev Tools", true);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }
}
