package vanstudio.sequence.agent;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.util.HashMap;
import java.util.Map;

public class ReadFileAgentEventHandler implements AgentEventHandler {
    @Override
    public String getOperationType() {
        return "readFile";
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> eventMap, Project project) {
        String path = (String) eventMap.get("path");
        path = CreateFileAgentEventHandler.getPath(path);
        PsiDirectory currentDirectory = PsiManager.getInstance(project).findDirectory(project.getBaseDir());
        String[] paths = path.split("/");
        for (int i = 0; i < paths.length - 1; i++) {
            currentDirectory = currentDirectory.findSubdirectory(paths[i]);
            if (currentDirectory == null) {
                throw new NullPointerException("file is not exists.");
            }
        }
        PsiFile file = currentDirectory.findFile(paths[paths.length - 1]);
        if(file == null) {
            throw new NullPointerException("file is not exists.");
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("content", file.getText());
        return resultMap;
    }
}
