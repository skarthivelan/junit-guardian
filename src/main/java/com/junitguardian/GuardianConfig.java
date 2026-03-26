package com.junitguardian;

import java.nio.file.Path;

public class GuardianConfig {
    private final Path projectPath;
    private final String mode;
    private final String outputDir;
    private final String llmProvider;
    private final String apiKey;
    private final String ollamaUrl;
    private final String model;
    private final int minCoverage;
    private final boolean failOnMissing;
    private final String includePattern;
    private final String excludePattern;
    private final boolean verbose;

    private GuardianConfig(Builder b) {
        this.projectPath   = b.projectPath;
        this.mode          = b.mode;
        this.outputDir     = b.outputDir;
        this.llmProvider   = b.llmProvider;
        this.apiKey        = b.apiKey;
        this.ollamaUrl     = b.ollamaUrl;
        this.model         = b.model;
        this.minCoverage   = b.minCoverage;
        this.failOnMissing = b.failOnMissing;
        this.includePattern= b.includePattern;
        this.excludePattern= b.excludePattern;
        this.verbose       = b.verbose;
    }

    public static Builder builder() { return new Builder(); }

    // --- Getters ---
    public Path   getProjectPath()   { return projectPath; }
    public String getMode()          { return mode; }
    public String getOutputDir()     { return outputDir; }
    public String getLlmProvider()   { return llmProvider; }
    public String getApiKey()        { return apiKey; }
    public String getOllamaUrl()     { return ollamaUrl; }
    public String getModel()         { return model; }
    public int    getMinCoverage()   { return minCoverage; }
    public boolean isFailOnMissing() { return failOnMissing; }
    public String getIncludePattern(){ return includePattern; }
    public String getExcludePattern(){ return excludePattern; }
    public boolean isVerbose()       { return verbose; }

    public static class Builder {
        private Path projectPath;
        private String mode = "scan";
        private String outputDir = "src/test/java";
        private String llmProvider = "anthropic";
        private String apiKey = "";
        private String ollamaUrl = "http://localhost:11434";
        private String model = "claude-sonnet-4-20250514";
        private int minCoverage = 80;
        private boolean failOnMissing = true;
        private String includePattern = "**/*.java";
        private String excludePattern = "";
        private boolean verbose = false;

        public Builder projectPath(Path v)    { this.projectPath = v; return this; }
        public Builder mode(String v)          { this.mode = v; return this; }
        public Builder outputDir(String v)     { this.outputDir = v; return this; }
        public Builder llmProvider(String v)   { this.llmProvider = v; return this; }
        public Builder apiKey(String v)        { this.apiKey = v; return this; }
        public Builder ollamaUrl(String v)     { this.ollamaUrl = v; return this; }
        public Builder model(String v)         { this.model = v; return this; }
        public Builder minCoverage(int v)      { this.minCoverage = v; return this; }
        public Builder failOnMissing(boolean v){ this.failOnMissing = v; return this; }
        public Builder includePattern(String v){ this.includePattern = v; return this; }
        public Builder excludePattern(String v){ this.excludePattern = v; return this; }
        public Builder verbose(boolean v)      { this.verbose = v; return this; }

        public GuardianConfig build() { return new GuardianConfig(this); }
    }
}
