package com.junitguardian.llm;

import com.google.gson.*;
import com.junitguardian.GuardianConfig;
import com.junitguardian.model.SourceFile;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Calls the Anthropic Messages API (claude-sonnet-4-*).
 */
public class AnthropicLlmClient implements LlmClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final MediaType JSON  = MediaType.get("application/json");
    private static final int MAX_TOKENS  = 4096;

    private final GuardianConfig config;
    private final OkHttpClient   http;
    private final Gson           gson;

    public AnthropicLlmClient(GuardianConfig config) {
        this.config = config;
        this.http   = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }

    @Override
    public String generateTests(SourceFile sf) throws Exception {
        validateApiKey();

        JsonObject body = new JsonObject();
        body.addProperty("model", config.getModel());
        body.addProperty("max_tokens", MAX_TOKENS);
        body.addProperty("system", PromptBuilder.systemPrompt());

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", PromptBuilder.userPrompt(sf));
        messages.add(userMsg);
        body.add("messages", messages);

        Request req = new Request.Builder()
            .url(API_URL)
            .addHeader("x-api-key", config.getApiKey())
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(gson.toJson(body), JSON))
            .build();

        try (Response resp = http.newCall(req).execute()) {
            String responseBody = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                throw new IOException("Anthropic API error " + resp.code() + ": " + responseBody);
            }
            return extractText(responseBody);
        }
    }

    private String extractText(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        JsonArray content = obj.getAsJsonArray("content");
        if (content == null || content.isEmpty()) return "";
        // First text block
        for (JsonElement el : content) {
            JsonObject block = el.getAsJsonObject();
            if ("text".equals(block.get("type").getAsString())) {
                return stripMarkdownFences(block.get("text").getAsString());
            }
        }
        return "";
    }

    private String stripMarkdownFences(String text) {
        // Remove ```java ... ``` wrappers if the model included them
        return text.replaceAll("^```java\\s*", "")
                   .replaceAll("^```\\s*", "")
                   .replaceAll("```\\s*$", "")
                   .trim();
    }

    private void validateApiKey() {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException(
                "Anthropic API key is required. Set ANTHROPIC_API_KEY env var or pass --api-key.");
        }
    }
}
