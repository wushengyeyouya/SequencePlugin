package vanstudio.sequence.agent;

import com.google.gson.internal.LazilyParsedNumber;
import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.concurrency.NonUrgentExecutor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import vanstudio.sequence.SequenceService;
import vanstudio.sequence.ui.TaskUI;
import vanstudio.sequence.util.HttpUtils;
import vanstudio.sequence.util.Utils;

import javax.swing.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CreateNewTaskAction extends AnAction {

    protected final Logger LOGGER = Logger.getInstance(getClass());

    public CreateNewTaskAction() {
//        super("Create AgentTask", "Create a new development agent task.", AllIcons.Actions.AddFile);
    }

    /**
     * only accept "", designation or pseudocode.
     * @return 从生成设计文档开始
     */
    protected String getRequirementDevProcess() {
        return "";
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }
        String requirementName = Messages.showInputDialog("Requirement name(需求名):", "Create New Requirement Task(创建新需求)",
                Messages.getQuestionIcon());
        if (StringUtils.isBlank(requirementName)) {
            JOptionPane.showMessageDialog(null, "Requirement name cannot be empty(需求名不能为空！)", "Create New Requirement Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("requirement_name", requirementName);
        requestBody.put("requirement_dev_process", getRequirementDevProcess());
        String branch = getCurrentGitBranch(project);
        requestBody.put("source_branch", branch);
        requestBody.put("feature_branch", branch);
        requestBody.put("app_name", project.getName());
        int taskId;
        try {
            Map<String, Object> responseBody = HttpUtils.post(Utils.getCreateTaskUrl(), null, requestBody);
            Utils.validateAgentResponse(responseBody);
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            LOGGER.info("created new BDP-Agent task " + data.get("requirement_id"));
            taskId = ((LazilyParsedNumber) data.get("requirement_id")).intValue();
        } catch (Exception e) {
            LOGGER.warn(e);
            JOptionPane.showMessageDialog(null, ExceptionUtil.getNonEmptyMessage(e, "Failed with no message."), "Create new requirement task", JOptionPane.ERROR_MESSAGE);
            return;
        }
        TaskUI taskUI = new TaskUI(project, Utils.getTaskUrl(taskId));
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(SequenceService.PLUGIN_NAME);
        if (toolWindow == null) {
            return;
        }
        ContentManager contentManager = toolWindow.getContentManager();
        final Content content = contentManager.getFactory().createContent(taskUI.getMainPanel(), requirementName, true);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
        ReadAction.nonBlocking(() -> afterTaskUICreated(anActionEvent, taskUI))
                .inSmartMode(project).submit(NonUrgentExecutor.getInstance());
    }

    protected void afterTaskUICreated(AnActionEvent anActionEvent, TaskUI taskUI) {
    }

    private String getCurrentGitBranch(Project project) {
        VcsRepositoryManager vcsRepositoryManager = VcsRepositoryManager.getInstance(project);
        Collection<Repository> repos =  vcsRepositoryManager.getRepositories();
        Repository repository;
        if (repos.isEmpty()) {
            repository = vcsRepositoryManager.getRepositoryForRoot(project.getBaseDir());
        } else {
            repository = (Repository) CollectionUtils.get(repos, 0);
        }
        return repository.getCurrentBranchName();
    }

}
