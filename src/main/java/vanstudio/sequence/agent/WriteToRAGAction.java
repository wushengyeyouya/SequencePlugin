package vanstudio.sequence.agent;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import vanstudio.sequence.ShowSequenceAction;
import vanstudio.sequence.util.HttpUtils;
import vanstudio.sequence.util.Utils;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WriteToRAGAction extends ShowSequenceAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null) return;
        final PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (psiFile != null) {
            String content = psiFile.getText();
            String path = psiFile.getVirtualFile().getPath();
            String projectName = project.getName();
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("appName", projectName);
            requestBody.put("content", content);
            requestBody.put("path", path.replace(Objects.requireNonNull(project.getBasePath()), ""));
            try {
                Map<String, Object> responseBody = HttpUtils.post(Utils.getWriteToRAGUrl(), null, requestBody);
                Utils.validateAgentResponse(responseBody);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, ExceptionUtil.getNonEmptyMessage(e, "Failed with no message."), "Write To RAG", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(null, "Write to RAG succeed!", "Write To RAG", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
