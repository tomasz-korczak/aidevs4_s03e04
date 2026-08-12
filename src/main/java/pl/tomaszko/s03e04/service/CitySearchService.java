package pl.tomaszko.s03e04.service;

import org.springframework.stereotype.Service;
import pl.tomaszko.s03e04.prompt.PromptTemplates;

@Service
public class CitySearchService {

    private final LlmSearchGateway llmSearchGateway;
    private final PromptTemplates promptTemplates;

    public CitySearchService(LlmSearchGateway llmSearchGateway, PromptTemplates promptTemplates) {
        this.llmSearchGateway = llmSearchGateway;
        this.promptTemplates = promptTemplates;
    }

    public SearchOutcome search(String params) {
        return llmSearchGateway.search("/api/city", promptTemplates.citySystemPrompt(), params);
    }
}
