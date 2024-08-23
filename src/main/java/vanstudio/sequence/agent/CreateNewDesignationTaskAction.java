package vanstudio.sequence.agent;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import vanstudio.sequence.ui.TaskUI;

import java.util.HashMap;
import java.util.Map;

public class CreateNewDesignationTaskAction extends CreateNewTaskAction implements DumbAware {

    /**
     * only accept "", designation or pseudocode.
     * @return 从生成设计文档开始
     */
    protected String getRequirementDevProcess() {
        return "designation";
    }

    @Override
    protected void afterTaskUICreated(AnActionEvent anActionEvent, TaskUI taskUI) {
        final PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (psiFile != null) {
            String content = psiFile.getText();
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("operationType", "fillTextarea");
            eventMap.put("content", content);
            taskUI.sendEvent(eventMap);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(psiElement != null && psiElement.getContainingFile().getName().endsWith(".md"));
    }
}
