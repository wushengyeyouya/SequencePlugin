package vanstudio.sequence.agent;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import vanstudio.sequence.config.SequenceParamsState;
import vanstudio.sequence.ui.TaskUI;

public class RefreshAgentUrlAction extends AnAction {
    private TaskUI taskUI;

    public RefreshAgentUrlAction(TaskUI taskUI) {
        super("Refresh Page", "Refresh Index page.", AllIcons.Actions.Refresh);
        this.taskUI = taskUI;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Logger.getInstance(RefreshAgentUrlAction.class)
                .info("try to refresh BDP-Agent with url: " + SequenceParamsState.getInstance().agentUrl);
        taskUI.reload();
    }
}
