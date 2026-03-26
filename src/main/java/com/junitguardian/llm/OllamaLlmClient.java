package com.junitguardian.llm;

import com.google.gson.*;
import com.junitguardian.GuardianConfig;
import com.junitguardian.model.SourceFile;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Calls a local Ollama instance (e.g. codellama, deepseek-coder, qwen2.5-coder).
 *
 * Resilience:
 *  - If the requested model returns 404, we auto-discover the first available
 *    model from /api/tags and retry once rather than crashing.
 *  - If Ollama has no models at all, we return null (guardian logs a warning,
 *    commit is not blocked in warn-only mode).
 */
public class OllamaLlmClient implements LlmClient {

    private static final MediaType JSON = MediaType.get("application/json");

    // Preferred fallback order when the configured model isn't found
    private static final List<String> PREFERRED_MODELS = List.of(
        "deepseek-coder", "qwen2.5-coder", "codellama", "mistral",
        "llama3", "llama2", "gemma", "phi"
    );

    private final GuardianConfig config;
    private final OkHttpClient   http;
    private final Gson           gson;
    private       String         resolvedModel; // set on first call

    public OllamaLlmClient(GuardianConfig config) {
        this.config        = config;
        this.resolvedModel = config.getModel();
        this.http          = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }

    @Override
    public String generateTests(SourceFile sf) throws Exception {
        // Lazy-resolve the model on the very first call
        if (resolvedModel == null || resolvedModel.isBlank()) {
            resolvedModel = config.getModel();
        }

        try {
            return callOllama(resolvedModel, sf);
        } catch (ModelNotFoundException e) {
            // The configured model isn't pulled — find something that is
            String fallback = findAvailableModel();
            if (fallback == null) {
                System.err.println("  [guardian] No Ollama models found on this machine.");
                System.err.println("             Pull one with:  ollama pull deepseek-coder");
                return null;
            }
            System.err.println("  [guardian] Model '" + resolvedModel + "' not found. "
                + "Switching to '" + fallback + "' for this session.");
            resolvedModel = fallback; // reuse on subsequent files
            return callOllama(resolvedModel, sf);
        }
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private String callOllama(String model, SourceFile sf) throws Exception {
        String url   = baseUrl() + "/api/generate";
        String prompt = PromptBuilder.systemPrompt() + "\n\n" + PromptBuilder.userPrompt(sf);

        JsonObject body = new JsonObject();
        body.addProperty("model",  model);
        body.addProperty("prompt", prompt);
        body.addProperty("stream", false);

        Request req = new Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(gson.toJson(body), JSON))
            .build();

        try (Response resp = http.newCall(req).execute()) {
            String rb = resp.body() != null ? resp.body().string() : "";

            if (resp.code() == 404) {
                // Ollama returns 404 when the model isn't pulled locally
                throw new ModelNotFoundException(model);
            }
            if (!resp.isSuccessful()) {
                throw new IOException("Ollama error " + resp.code() + ": " + rb);
            }

            JsonObject obj = JsonParser.parseString(rb).getAsJsonObject();
            String text = obj.get("response").getAsString();
            return stripFences(text);
        }
    }

    /**
     * Ask Ollama for all locally-available models and return the best match
     * from PREFERRED_MODELS, or the first one found, or null if none.
     */
    private String findAvailableModel() {
        try {
            Request req = new Request.Builder()
                .url(baseUrl() + "/api/tags")
                .get()
                .build();

            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return null;
                String body = resp.body().string();
                JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                JsonArray models = obj.getAsJsonArray("models");
                if (models == null || models.isEmpty()) return null;

                List<String> available = new ArrayList<>();
                for (JsonElement el : models) {
                    String name = el.getAsJsonObject().get("name").getAsString();
                    // Strip the tag suffix (e.g. "codellama:latest" → "codellama")
                    available.add(name.contains(":") ? name.substring(0, name.indexOf(':')) : name);
                }

                // Return the highest-priority preferred model that is available
                for (String preferred : PREFERRED_MODELS) {
                    if (available.contains(preferred)) return preferred;
                }

                // Fall back to whatever is first in the list
                return models.get(0).getAsJsonObject().get("name").getAsString();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String baseUrl() {
        return config.getOllamaUrl().replaceAll("/$", "");
    }

    private String stripFences(String text) {
        return text.replaceAll("(?s)^```java\\s*", "")
                   .replaceAll("(?s)^```\\s*",     "")
                   .replaceAll("(?s)```\\s*$",      "")
                   .trim();
    }

    // ── sentinel exception ─────────────────────────────────────────────────────

    private static class ModelNotFoundException extends RuntimeException {
        ModelNotFoundException(String model) {
            super("Model not found locally: " + model);
        }
    }
}
