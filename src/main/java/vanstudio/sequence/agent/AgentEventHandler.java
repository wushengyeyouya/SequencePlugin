package vanstudio.sequence.agent;

import com.intellij.openapi.project.Project;

import java.util.Map;

public interface AgentEventHandler {

    String getOperationType();

    Map<String, Object> handle(Map<String, Object> eventMap, Project project);

}
