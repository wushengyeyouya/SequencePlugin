package vanstudio.sequence.agent;

import com.intellij.openapi.actionSystem.AnActionEvent;
import vanstudio.sequence.util.Utils;

import java.util.Map;

import static vanstudio.sequence.agent.CreateFileAgentEventHandler.CREATE_FILE_OPERATION_TYPE;

public class GenAllDevDocsAction extends GenerateReadmeAction {

    @Override
    protected void setParams(Map<String, Object> paramsMap, AnActionEvent anActionEvent) {
        super.setParams(paramsMap, anActionEvent);
        paramsMap.put("operationType", "genAllDevDocs");
        paramsMap.remove("uri");
    }

    @Override
    protected String getUiTitle() {
        return "自动生成所有设计文档";
    }
}
