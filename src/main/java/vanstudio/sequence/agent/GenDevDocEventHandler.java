package vanstudio.sequence.agent;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.NonUrgentExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.CancellablePromise;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import vanstudio.sequence.formatter.JsonFormatter;
import vanstudio.sequence.formatter.PlantUMLFormatter;
import vanstudio.sequence.openapi.GeneratorFactory;
import vanstudio.sequence.openapi.IGenerator;
import vanstudio.sequence.openapi.SequenceParams;
import vanstudio.sequence.openapi.model.CallStack;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class GenDevDocEventHandler implements AgentEventHandler{
    @Override
    public String getOperationType() {
        return "genDevDoc";
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> eventMap, Project project) {
        String classPath = (String) eventMap.get("class_path");
        String methodName = (String) eventMap.get("method_name");
        String[] paths;
        if (classPath.indexOf("/") > 0) {
            paths = classPath.split("/");
        } else {
            paths = classPath.split("\\\\");
        }
        CancellablePromise<Map<String, Object>> promise = ReadAction.nonBlocking(() -> {
            PsiDirectory currentDirectory = PsiManager.getInstance(project).findDirectory(project.getBaseDir());
            for (int i = 0; i < paths.length - 1; i++) {
                currentDirectory = currentDirectory.findSubdirectory(paths[i]);
                if (currentDirectory == null) {
                    throw new NullPointerException("dir " + paths[i] + " is not exists.");
                }
            }
            PsiFile file = currentDirectory.findFile(paths[paths.length - 1]);
            if(file == null) {
                throw new NullPointerException("file " + paths[paths.length - 1] + " is not exists.");
            }
            List<UMethod> uMethods = new ArrayList<>();
            UElement uElement = UastContextKt.toUElement(file);
            if (uElement != null) {
                uElement.accept(new AbstractUastVisitor() {
                    @Override
                    public boolean visitClass(@NotNull UClass node) {
                        Optional<UMethod> uMethodOptional = Arrays.stream(node.getMethods()).filter(uMethod -> uMethod.getName().equals(methodName)).findFirst();
                        uMethodOptional.ifPresent(uMethods::add);
                        return false;
                    }
                });
            }
            if(uMethods.isEmpty()) {
                throw new NullPointerException("cannot find method " + methodName + " in classPath " + classPath);
            }
            SequenceParams _sequenceParams = new SequenceParams();
            _sequenceParams.setShowEditableChooseDialog(false);
            IGenerator generator = GeneratorFactory.createGenerator(uMethods.get(0).getLanguage(), _sequenceParams);
            CallStack callStack = generator.generate(uMethods.get(0), null);
            Map<String, Object> requestBody = new HashMap<>();
            String sequence_json = new JsonFormatter().format(callStack);
            String plant_uml = new PlantUMLFormatter().format(callStack);
            requestBody.put("app_name", project.getName());
            requestBody.put("sequence_json", sequence_json);
            requestBody.put("plant_uml", plant_uml);
            return requestBody;
        }).submit(NonUrgentExecutor.getInstance());
        try {
            return promise.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
