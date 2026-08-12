package pl.tomaszko.s03e04.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import pl.tomaszko.s03e04.logging.ModelExchangeLogger;

@Service
public class LlmSearchGateway {

    private static final Logger log = LoggerFactory.getLogger(LlmSearchGateway.class);

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;
    private final OutputConstraintValidator outputConstraintValidator;
    private final ModelExchangeLogger modelExchangeLogger;

    public LlmSearchGateway(
            ChatClient chatClient,
            ToolCallbackProvider toolCallbackProvider,
            OutputConstraintValidator outputConstraintValidator,
            ModelExchangeLogger modelExchangeLogger) {
        this.chatClient = chatClient;
        this.toolCallbackProvider = toolCallbackProvider;
        this.outputConstraintValidator = outputConstraintValidator;
        this.modelExchangeLogger = modelExchangeLogger;
    }

    public SearchOutcome search(String endpoint, String systemPrompt, String userParams) {
        ToolCallback[] tools = toolCallbackProvider.getToolCallbacks();
        modelExchangeLogger.logRequest(endpoint, systemPrompt, tools, userParams);
        try {
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userParams)
                    .call()
                    .content();
            modelExchangeLogger.logResponse(endpoint, content);
            String enforced = outputConstraintValidator.enforce(normalizeModelContent(content));
            return SearchOutcome.ok(enforced);
        } catch (RuntimeException ex) {
            log.error("LLM/MCP infrastructure failure on {}: {}", endpoint, ex.getMessage(), ex);
            String message = outputConstraintValidator.enforce(summarizeInfraError(ex));
            return SearchOutcome.infrastructure(message);
        }
    }

    private static String normalizeModelContent(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNl = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                trimmed = trimmed.substring(firstNl + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private static String summarizeInfraError(Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "LLM or MCP tools failed";
        }
        String lower = msg.toLowerCase();
        if (lower.contains("401") || lower.contains("403") || lower.contains("auth")) {
            return "LLM provider unavailable";
        }
        if (lower.contains("mcp")) {
            return "MCP file tools failed";
        }
        if (msg.length() > 120) {
            return "LLM provider unavailable";
        }
        return msg;
    }
}
