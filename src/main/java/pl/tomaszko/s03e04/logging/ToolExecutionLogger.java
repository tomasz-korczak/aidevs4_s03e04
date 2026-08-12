package pl.tomaszko.s03e04.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutionLogger {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionLogger.class);

    public ToolCallback wrap(ToolCallback delegate) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return delegate.getToolDefinition();
            }

            @Override
            public String call(String toolInput) {
                String name = delegate.getToolDefinition().name();
                log.info("TOOL_CALL name={} params={}", name, toolInput);
                try {
                    String result = delegate.call(toolInput);
                    log.info("TOOL_RESULT name={} result={}", name, result);
                    return result;
                } catch (RuntimeException ex) {
                    log.error("TOOL_ERROR name={} message={}", name, ex.getMessage(), ex);
                    throw ex;
                }
            }
        };
    }

    public ToolCallback[] wrapAll(ToolCallback[] callbacks) {
        if (callbacks == null || callbacks.length == 0) {
            return callbacks;
        }
        ToolCallback[] wrapped = new ToolCallback[callbacks.length];
        for (int i = 0; i < callbacks.length; i++) {
            wrapped[i] = wrap(callbacks[i]);
        }
        return wrapped;
    }
}
