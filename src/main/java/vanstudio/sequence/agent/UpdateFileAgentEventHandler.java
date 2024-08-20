package vanstudio.sequence.agent;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;

import java.util.Map;

public class UpdateFileAgentEventHandler implements AgentEventHandler {

    public static final String UPDATE_FILE_OPERATION_TYPE = "updateFile";

    @Override
    public String getOperationType() {
        return UPDATE_FILE_OPERATION_TYPE;
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> eventMap, Project project) {
        String location = (String) eventMap.get("path");
        String path = CreateFileAgentEventHandler.getPath(location);
        String content = (String) eventMap.get("content");
        WriteCommandAction.runWriteCommandAction(project, () -> {
            // Split the full path into directories and file name
            String[] parts = path.split("/");
            StringBuilder dirPath = new StringBuilder();
            PsiDirectory currentDirectory = PsiManager.getInstance(project).findDirectory(project.getBaseDir());

            // Create missing directories
            for (int i = 0; i < parts.length - 1; i++) {
                dirPath.append("/").append(parts[i]);
                PsiDirectory childDirectory = currentDirectory.findSubdirectory(parts[i]);
                if (childDirectory == null) {
                    throw new RuntimeException("path " + dirPath + " does not exists.");
                }
                currentDirectory = childDirectory;
            }

            // find the file
            PsiFile psiFile = currentDirectory.findFile(parts[parts.length - 1]);
            if (psiFile == null) {
                throw new RuntimeException("file " + parts[parts.length - 1] + " does not exists.");
            }

            psiFile.replace(PsiFileFactory.getInstance(project)
                    .createFileFromText(parts[parts.length - 1], CreateFileAgentEventHandler.getFileType(parts[parts.length - 1]), content));
            // Open the file in the editor
            ApplicationManager.getApplication().invokeLater(() -> psiFile.navigate(true));

        });
        return null;
    }
}
