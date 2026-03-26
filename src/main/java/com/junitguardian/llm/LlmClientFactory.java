package com.junitguardian.llm;

import com.junitguardian.GuardianConfig;

public class LlmClientFactory {
    public static LlmClient create(GuardianConfig config) {
        switch (config.getLlmProvider().toLowerCase()) {
            case "anthropic": return new AnthropicLlmClient(config);
            case "openai":    return new OpenAiLlmClient(config);
            case "ollama":    return new OllamaLlmClient(config);
            default:
                throw new IllegalArgumentException(
                    "Unknown LLM provider: " + config.getLlmProvider()
                    + ". Supported: anthropic, openai, ollama");
        }
    }
}
