package vanstudio.sequence.formatter;

import org.apache.commons.lang3.StringUtils;
import vanstudio.sequence.config.SequenceSettingsState;
import vanstudio.sequence.openapi.Constants;
import vanstudio.sequence.openapi.model.CallStack;
import vanstudio.sequence.openapi.model.LambdaExprDescription;
import vanstudio.sequence.openapi.model.MethodDescription;
import vanstudio.sequence.util.Utils;

import java.util.*;
import java.util.function.Consumer;

/**
 *
 */
public class JsonFormatter implements IFormatter{
    @Override
    public String format(CallStack callStack) {
        Map<String, Object> callStackMap = generate(callStack);
        return Utils.gson.toJson(callStackMap);
    }

    private void participant(Set<String> participantSet, CallStack parent) {
        Consumer<CallStack> addOrIgnore = callStack -> {
            String className = callStack.getMethod().getClassDescription().getClassName();
            if (StringUtils.isNotBlank(className) &&
                    !Constants.ANONYMOUS_CLASS_NAME.equals(className)) {
                participantSet.add(className.trim());
            }
        };
        addOrIgnore.accept(parent);
        parent.getCalls().forEach(callStack -> participant(participantSet, callStack));
    }

    private Map<String, Object> generate(CallStack parent) {
        String classA = parent.getMethod().getClassDescription().getClassName();
        Map<String, Object> callStackMap = new HashMap<>();
        callStackMap.put("className", classA);
        String method = getMethodName(parent.getMethod());
        if (StringUtils.isBlank(method)) {
            return null;
        }
        callStackMap.put("method", method);
        String key = getKey(callStackMap);
        if (Constants.CONSTRUCTOR_METHOD_NAME.equals(parent.getMethod().getMethodName())) {
            callStackMap.put("isConstructor", true);
        }
        Map<String, Map<String, Object>> childCallStackMaps = new HashMap<>();
        for (CallStack callStack : parent.getCalls()) {
            Map<String, Object> childCallStackMap = generate(callStack);
            if (childCallStackMap == null) {
                continue;
            }
            String childKey = getKey(childCallStackMap);
            if (key.equals(childKey)) {
                List<Map<String, Object>> childChildrenCallStackList = (List<Map<String, Object>>) childCallStackMap.get("calls");
                if (childChildrenCallStackList.isEmpty()) {
                    continue;
                }
                childChildrenCallStackList.forEach(map -> {
                    String childChildKey = getKey(map);
                    if (!key.equals(childChildKey) && !childCallStackMaps.containsKey(childChildKey)) {
                        childCallStackMaps.put(childChildKey, map);
                    }
                });
            } else {
                childCallStackMaps.put(childKey, childCallStackMap);
            }
        }
        callStackMap.put("calls", new ArrayList<>(childCallStackMaps.values()));
        return callStackMap;
    }

    private String getKey(Map<String, Object> callStackMap) {
        return callStackMap.get("className") + "." + callStackMap.get("method");
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
