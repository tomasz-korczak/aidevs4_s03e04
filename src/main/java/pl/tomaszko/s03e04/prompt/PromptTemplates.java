package pl.tomaszko.s03e04.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import pl.tomaszko.s03e04.config.AppProperties;

@Component
public class PromptTemplates {

    private final ResourceLoader resourceLoader;
    private final AppProperties appProperties;

    public PromptTemplates(ResourceLoader resourceLoader, AppProperties appProperties) {
        this.resourceLoader = resourceLoader;
        this.appProperties = appProperties;
    }

    public String citySystemPrompt() {
        return render(appProperties.getPrompts().getCity().getTemplate());
    }

    public String itemsSystemPrompt() {
        return render(appProperties.getPrompts().getItems().getTemplate());
    }

    public String render(String location) {
        String template = readTemplate(location);
        Map<String, String> values = Map.of(
                "data_root", appProperties.getMcp().getFiles().getDataRoot(),
                "byte_min", String.valueOf(appProperties.getOutput().getByteMin()),
                "byte_max", String.valueOf(appProperties.getOutput().getByteMax())
        );
        String rendered = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return rendered;
    }

    private String readTemplate(String location) {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load prompt template: " + location, ex);
        }
    }
}
