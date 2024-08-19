package vanstudio.sequence.formatter;

import org.apache.commons.lang3.StringUtils;
import vanstudio.sequence.config.SequenceSettingsState;
import vanstudio.sequence.openapi.Constants;
import vanstudio.sequence.openapi.model.CallStack;
import vanstudio.sequence.openapi.model.LambdaExprDescription;
import vanstudio.sequence.openapi.model.MethodDescription;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Generate <a href="https://plantuml.com/sequence-diagram">PlantUml sequence diagram</a> format.
 *
 */
public class PlantUMLFormatter implements IFormatter{
    @Override
    public String format(CallStack callStack) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("@startuml").append('\n');
        buffer.append("participant Actor").append('\n');
        Set<String> participantSet = new HashSet<>();
        participant(participantSet, callStack);
        if(!participantSet.isEmpty()) {
            buffer.append(String.join("\n", participantSet)).append('\n');
        }
        String classA = callStack.getMethod().getClassDescription().getClassShortName();
        String method = getMethodName(callStack.getMethod());
        if (Constants.CONSTRUCTOR_METHOD_NAME.equals(callStack.getMethod().getMethodName())) {
            buffer.append("create ").append(classA).append('\n');
        }
        buffer.append("Actor").append(" -> ").append(classA).append(" : ").append(method).append('\n');
        buffer.append("activate ").append(classA).append('\n');
        generate(buffer, callStack);
        buffer.append("return").append('\n');
        buffer.append("@enduml");
        return buffer.toString();
    }

    private void participant(Set<String> participantSet, CallStack parent) {
        Consumer<CallStack> addOrIgnore = callStack -> {
            String className = callStack.getMethod().getClassDescription().getClassName();
            if (StringUtils.isNotBlank(className) &&
                    !Constants.ANONYMOUS_CLASS_NAME.equals(className)) {
                participantSet.add("participant " + className.trim());
            }
        };
        addOrIgnore.accept(parent);
        parent.getCalls().forEach(callStack -> participant(participantSet, callStack));
    }

    private void generate(StringBuffer buffer, CallStack parent) {
        String classA = parent.getMethod().getClassDescription().getClassShortName();

        for (CallStack callStack : parent.getCalls()) {
            String classB = callStack.getMethod().getClassDescription().getClassShortName();
            String method = getMethodName(callStack.getMethod());
            if (StringUtils.isBlank(method)) {
                continue;
            }
            if (Constants.CONSTRUCTOR_METHOD_NAME.equals(callStack.getMethod().getMethodName())) {
                buffer.append("create ").append(classB).append('\n');
            }
            buffer.append(classA).append(" -> ").append(classB).append(" : ").append(method).append('\n');
            buffer.append("activate ").append(classB).append('\n');
            generate(buffer, callStack);
            buffer.append(classB).append(" --> ").append(classA).append('\n');
            buffer.append("deactivate ").append(classB).append('\n');
        }

    }

    private String getMethodName(MethodDescription method) {
        if (method == null) return "";

        if (method instanceof LambdaExprDescription) {
            return ((LambdaExprDescription) method).getEnclosedMethodName();
        }
        if (SequenceSettingsState.getInstance().SHOW_SIMPLIFY_CALL_NAME) {
            return method.getMethodName();
        } else {
            return method.getFullName();
        }

    }
}
