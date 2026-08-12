package pl.tomaszko.s03e04.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupReadinessValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupReadinessValidator.class);
    private static final List<String> REQUIRED_FILES = List.of("cities.csv", "items.csv", "connections.csv");

    private final AppProperties appProperties;
    private final ToolCallbackProvider toolCallbackProvider;

    public StartupReadinessValidator(AppProperties appProperties, ToolCallbackProvider toolCallbackProvider) {
        this.appProperties = appProperties;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path dataRoot = Path.of(appProperties.getMcp().getFiles().getDataRoot());
        if (!Files.isDirectory(dataRoot)) {
            throw new IllegalStateException("Data root is missing or not a directory: " + dataRoot);
        }
        for (String fileName : REQUIRED_FILES) {
            Path file = dataRoot.resolve(fileName);
            if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                throw new IllegalStateException("Required corpus file missing or unreadable: " + file);
            }
        }

        Path jar = Path.of(appProperties.getMcp().getFiles().getJarPath());
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException("filesmcp JAR missing: " + jar);
        }

        ToolCallback[] tools = toolCallbackProvider.getToolCallbacks();
        if (tools == null || tools.length == 0) {
            throw new IllegalStateException("MCP client initialized but no read tools (fs_read/fs_search) are available");
        }
        log.info("Startup readiness OK: corpus at {}, MCP tools={}", dataRoot, tools.length);
    }
}
