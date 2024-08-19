package vanstudio.sequence.agent;

import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.Map;

import static vanstudio.sequence.agent.CreateFileAgentEventHandler.CREATE_FILE_OPERATION_TYPE;
import static vanstudio.sequence.util.Utils.gson;

public class AgentEventHandlerFactory {

    private static final Map<String, AgentEventHandler> agentEventHandlers = new HashMap<>();
    static {
        agentEventHandlers.put(CREATE_FILE_OPERATION_TYPE, new CreateFileAgentEventHandler());
        agentEventHandlers.put("readFile", new ReadFileAgentEventHandler());
        agentEventHandlers.put(UpdateFileAgentEventHandler.UPDATE_FILE_OPERATION_TYPE, new UpdateFileAgentEventHandler());
    }

    public static String handle(String eventStr, Project project) {
        Map<String, Object> eventMap = gson.fromJson(eventStr, Map.class);
        return handle(eventMap, project);
    }

    public static String handle(Map<String, Object> eventMap, Project project) {
        String operationType = (String) eventMap.get("operationType");
        String id = (String) eventMap.get("operationId");
        if (!agentEventHandlers.containsKey(operationType)) {
            throw new IllegalArgumentException("not exists operationType " + operationType);
        }
        Map<String, Object> handleResultMap =  agentEventHandlers.get(operationType).handle(eventMap, project);
        if (handleResultMap == null) {
            handleResultMap = new HashMap<>();
        }
        handleResultMap.put("operationId", id);
        return gson.toJson(handleResultMap);
    }

}
