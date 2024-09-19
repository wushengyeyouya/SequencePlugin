package vanstudio.sequence.agent;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.concurrency.NonUrgentExecutor;
import org.jetbrains.annotations.NotNull;
import vanstudio.sequence.ShowSequenceAction;
import vanstudio.sequence.util.HttpUtils;
import vanstudio.sequence.util.Utils;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static vanstudio.sequence.agent.CreateFileAgentEventHandler.CREATE_FILE_OPERATION_TYPE;

public class GenerateReadmeAction extends ShowSequenceAction {

    private Logger logger = Logger.getInstance(GenerateReadmeAction.class);

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
        if (project == null) return;
        BackgroundableProcessIndicator progressIndicator =
                new BackgroundableProcessIndicator(
                        project,
                        "Generate readme...",
                        PerformInBackgroundOption.ALWAYS_BACKGROUND,
                        "Stop",
                        "Stop",
                        false);
        String projectName = project.getName();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("app_name", projectName);
        ReadAction.nonBlocking(() -> {
                    try {
                        Map<String, Object> responseBody = HttpUtils.post(Utils.getGenerateReadmeUrl(), null, requestBody);
                        Utils.validateAgentResponse(responseBody);
                        Map<String, Object> contentMap = new HashMap<>();
                        contentMap.put("path", "README-ZH.md");
                        contentMap.put("operationType", CREATE_FILE_OPERATION_TYPE);
                        contentMap.put("content", ((Map<String, Object>) responseBody.get("data")).get("readme"));
                        contentMap.put("overwrite", true);
                        AgentEventHandlerFactory.handle(contentMap, anActionEvent.getProject());
                        progressIndicator.processFinish();
                        JOptionPane.showMessageDialog(null, "Generate README-ZH.md succeed!", "Generate README", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        progressIndicator.processFinish();
                        throw new RuntimeException(e);
//                        logger.warn(e);
//                        JOptionPane.showMessageDialog(null, ExceptionUtil.getNonEmptyMessage(e, "Failed with no message."), "Generate README", JOptionPane.ERROR_MESSAGE);
                    }
        }).wrapProgress(progressIndicator)
                .inSmartMode(project)
                .submit(NonUrgentExecutor.getInstance());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
