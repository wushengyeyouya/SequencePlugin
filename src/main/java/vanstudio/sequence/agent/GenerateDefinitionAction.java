package vanstudio.sequence.agent;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
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

public class GenerateDefinitionAction extends ShowSequenceAction {

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
                        "Generate definition...",
                        PerformInBackgroundOption.ALWAYS_BACKGROUND,
                        "Stop",
                        "Stop",
                        false);
        final PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        String projectName = project.getName();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("app_name", projectName);
//        ReadAction.nonBlocking(() -> {
                    try {
                        Map<String, Object> responseBody = HttpUtils.post(Utils.getGenerateDefinitionUrl(), null, requestBody);
                        Utils.validateAgentResponse(responseBody);
                        Map<String, Object> contentMap = new HashMap<>();
                        contentMap.put("path", "项目概念定义.md");
                        contentMap.put("operationType", CREATE_FILE_OPERATION_TYPE);
                        contentMap.put("content", ((Map<String, Object>) responseBody.get("data")).get("definition_docs"));
                        contentMap.put("overwrite", true);
                        AgentEventHandlerFactory.handle(contentMap, anActionEvent.getProject());
                        progressIndicator.processFinish();
                        JOptionPane.showMessageDialog(null, "Generate 项目概念定义.md succeed!", "Generate 项目概念定义文档", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        progressIndicator.processFinish();
                        JOptionPane.showMessageDialog(null, ExceptionUtil.getNonEmptyMessage(e, "Failed with no message."), "Generate 项目概念定义文档", JOptionPane.ERROR_MESSAGE);
                    }
//                }).wrapProgress(progressIndicator)
//                .inSmartMode(project)
//                .submit(NonUrgentExecutor.getInstance());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
