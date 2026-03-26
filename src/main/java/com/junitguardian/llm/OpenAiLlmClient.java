package com.junitguardian.llm;

import com.google.gson.*;
import com.junitguardian.GuardianConfig;
import com.junitguardian.model.SourceFile;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Calls the OpenAI Chat Completions API (gpt-4o, etc.).
 */
public class OpenAiLlmClient implements LlmClient {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON  = MediaType.get("application/json");

    private final GuardianConfig config;
    private final OkHttpClient   http;
    private final Gson           gson;

    public OpenAiLlmClient(GuardianConfig config) {
        this.config = config;
        this.http   = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
        this.gson   = new Gson();
    }

    @Override
    public String generateTests(SourceFile sf) throws Exception {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException(
                "OpenAI API key required. Set OPENAI_API_KEY env var or pass --api-key.");
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", config.getModel());
        body.addProperty("max_tokens", 4096);
        body.addProperty("temperature", 0.2);

        JsonArray messages = new JsonArray();

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", PromptBuilder.systemPrompt());
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", PromptBuilder.userPrompt(sf));
        messages.add(user);

        body.add("messages", messages);

        Request req = new Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(gson.toJson(body), JSON))
            .build();

        try (Response resp = http.newCall(req).execute()) {
            String rb = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) throw new IOException("OpenAI error " + resp.code() + ": " + rb);
            return extractText(rb);
        }
    }

    private String extractText(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return obj.getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString()
            .replaceAll("^```java\\s*", "").replaceAll("^```\\s*", "").replaceAll("```\\s*$", "")
            .trim();
    }
}
