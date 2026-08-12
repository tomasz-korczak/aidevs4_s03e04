package pl.tomaszko.s03e04.logging;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class ModelExchangeLogger {

    private static final Logger log = LoggerFactory.getLogger(ModelExchangeLogger.class);

    public void logRequest(String endpoint, String systemPrompt, ToolCallback[] tools, String userPrompt) {
        String toolDefs = tools == null || tools.length == 0
                ? "(none)"
                : Arrays.stream(tools)
                        .map(t -> t.getToolDefinition().name() + ": " + t.getToolDefinition().description())
                        .collect(Collectors.joining(" | "));
        log.info("MODEL_REQUEST endpoint={} systemPrompt={}", endpoint, systemPrompt);
        log.info("MODEL_TOOLS endpoint={} definitions={}", endpoint, toolDefs);
        log.info("MODEL_USER endpoint={} userPrompt={}", endpoint, userPrompt);
    }

    public void logResponse(String endpoint, String response) {
        log.info("MODEL_RESPONSE endpoint={} response={}", endpoint, response);
    }
}
