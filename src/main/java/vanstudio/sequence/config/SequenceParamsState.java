package vanstudio.sequence.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vanstudio.sequence.util.Utils;

import java.util.ArrayList;
import java.util.List;

@State(name = "sequenceParams", storages = {@Storage("sequencePlugin.xml")})
public class SequenceParamsState implements PersistentStateComponent<SequenceParamsState> {
    public int callDepth = 8;
    public boolean projectClassesOnly = true;
    public boolean noGetterSetters = true;
    public boolean noPrivateMethods = false;
    public boolean noConstructors = false;
    @Deprecated
    public boolean smartInterface = false;
    public String agentUrl = Utils.BDP_AGENT_URL;

    @Transient
    private final List<ConfigListener> _listeners = new ArrayList<>();

    public SequenceParamsState() {
    }

    public static @NotNull SequenceParamsState getInstance() {
        return ApplicationManager.getApplication().getService(SequenceParamsState.class);
    }

    public void addConfigListener(ConfigListener listener) {
        _listeners.add(listener);
    }

    public void removeConfigListener(ConfigListener listener) {
        _listeners.remove(listener);
    }

    public void fireConfigChanged() {
        for (ConfigListener configListener : _listeners) {
            configListener.configChanged();
        }
    }

    @Override
    public @Nullable SequenceParamsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SequenceParamsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
