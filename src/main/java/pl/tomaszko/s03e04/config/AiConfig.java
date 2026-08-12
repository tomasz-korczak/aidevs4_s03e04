package pl.tomaszko.s03e04.config;

import java.util.Arrays;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.ai.mcp.McpToolNamePrefixGenerator;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import pl.tomaszko.s03e04.logging.ToolExecutionLogger;

@Configuration
public class AiConfig {

    private static final Set<String> ALLOWED_TOOLS = Set.of("fs_read", "fs_search");

    @Bean
    public McpToolFilter readOnlyMcpToolFilter() {
        return (connectionInfo, tool) -> isAllowed(tool.name());
    }

    @Bean
    public McpToolNamePrefixGenerator mcpToolNamePrefixGenerator() {
        return McpToolNamePrefixGenerator.noPrefix();
    }

    @Bean
    @Primary
    public ToolCallbackProvider loggingToolCallbackProvider(
            SyncMcpToolCallbackProvider mcpToolCallbackProvider,
            ToolExecutionLogger toolExecutionLogger) {
        return () -> {
            ToolCallback[] callbacks = toolExecutionLogger.wrapAll(mcpToolCallbackProvider.getToolCallbacks());
            return Arrays.stream(callbacks)
                    .filter(cb -> isAllowed(cb.getToolDefinition().name()))
                    .toArray(ToolCallback[]::new);
        };
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ToolCallbackProvider loggingToolCallbackProvider) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(loggingToolCallbackProvider)
                .build();
    }

    private static boolean isAllowed(String name) {
        if (name == null) {
            return false;
        }
        return ALLOWED_TOOLS.contains(name)
                || ALLOWED_TOOLS.stream().anyMatch(allowed ->
                        name.equals(allowed) || name.endsWith("_" + allowed) || name.endsWith(allowed));
    }
}
