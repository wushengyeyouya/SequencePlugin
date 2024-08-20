package vanstudio.sequence.agent;

import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;

import java.util.Map;

public class CreateFileAgentEventHandler implements AgentEventHandler {

    public static final String CREATE_FILE_OPERATION_TYPE = "createFile";

    @Override
    public String getOperationType() {
        return CREATE_FILE_OPERATION_TYPE;
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> eventMap, Project project) {
        String location = (String) eventMap.get("path");
        String path = getPath(location);
        String content = (String) eventMap.get("content");
        boolean overwrite = eventMap.containsKey("overwrite") && (boolean) eventMap.get("overwrite");
        WriteCommandAction.runWriteCommandAction(project, () -> {
            // Split the full path into directories and file name
            String[] parts = path.split("/");
            PsiDirectory currentDirectory = PsiManager.getInstance(project).findDirectory(project.getBaseDir());

            // Create missing directories
            for (int i = 0; i < parts.length - 1; i++) {
                PsiDirectory childDirectory = currentDirectory.findSubdirectory(parts[i]);
                if (childDirectory == null) {
                    childDirectory = currentDirectory.createSubdirectory(parts[i]);
                }
                currentDirectory = childDirectory;
            }
            String fileName = parts[parts.length - 1];
            PsiFile psiFile = currentDirectory.findFile(fileName);
            if (psiFile != null && !overwrite) {
                throw new RuntimeException("file " + fileName + " is already exists.");
            }

            // Create the file
            PsiFile file = PsiFileFactory.getInstance(project)
                    .createFileFromText(fileName, getFileType(fileName), content);
            if (psiFile != null) {
                psiFile.replace(file);
            } else {
                currentDirectory.add(file);
            }
            // Open the file in the editor
            ApplicationManager.getApplication().invokeLater(() -> file.navigate(true));

        });
        return null;
    }

    public static FileType getFileType(String fileName) {
        if (fileName.endsWith(".java")) {
            return JavaFileType.INSTANCE;
        } else if(fileName.endsWith(".xml")) {
            return XmlFileType.INSTANCE;
        } else if(fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return HtmlFileType.INSTANCE;
        } else if (fileName.endsWith(".json")) {
            return JsonFileType.INSTANCE;
        } else {
            return PlainTextFileType.INSTANCE;
        }
    }

    public static String getPath(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        } else if (path.startsWith("./")) {
            return path.substring(2);
        }
        return path;
    }
}
