package vanstudio.sequence.agent;

import com.intellij.openapi.actionSystem.AnActionEvent;
import vanstudio.sequence.util.Utils;

import java.util.Map;

public class GenerateDefinitionAction extends GenerateReadmeAction {

    @Override
    protected void setParams(Map<String, Object> paramsMap, AnActionEvent anActionEvent) {
        super.setParams(paramsMap, anActionEvent);
        paramsMap.put("path", "项目概念定义.md");
        paramsMap.put("content_key", "definition_docs");
        paramsMap.put("uri", Utils.getGenerateDefinitionUri());
    }

    @Override
    protected String getUiTitle() {
        return "Generate 项目概念定义文档";
    }
}
