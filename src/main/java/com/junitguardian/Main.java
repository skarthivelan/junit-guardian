package com.junitguardian;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
    name = "junit-guardian",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "LLM-powered JUnit test scanner and generator for Java projects"
)
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the Java project root", defaultValue = ".")
    private Path projectPath;

    @Option(names = {"-m", "--mode"},
            description = "Mode: 'scan' (check only), 'generate' (create tests), 'report' (full report). Default: generate",
            defaultValue = "generate")
    private String mode;

    @Option(names = {"-o", "--output"},
            description = "Output directory for generated test files",
            defaultValue = "src/test/java")
    private String outputDir;

    @Option(names = {"--llm-provider"},
            description = "LLM provider: anthropic, openai, ollama. Default: ollama",
            defaultValue = "ollama")
    private String llmProvider;

    @Option(names = {"--api-key"},
            description = "API key for the LLM provider (or set via env var: ANTHROPIC_API_KEY / OPENAI_API_KEY)",
            defaultValue = "")
    private String apiKey;

    @Option(names = {"--ollama-url"},
            description = "Ollama base URL (default: http://localhost:11434)",
            defaultValue = "http://localhost:11434")
    private String ollamaUrl;

    @Option(names = {"--model"},
            description = "Model name to use (defaults to provider best model)",
            defaultValue = "")
    private String model;

    @Option(names = {"--min-coverage"},
            description = "Minimum required test coverage percentage (0-100). Default: 80",
            defaultValue = "80")
    private int minCoverage;

    @Option(names = {"--fail-on-missing"},
            description = "Exit with code 1 if coverage is below threshold. Default: false (warn only)",
            defaultValue = "false")
    private boolean failOnMissing;

    @Option(names = {"--include-pattern"},
            description = "Glob pattern for Java files to include. Default: **/*.java",
            defaultValue = "**/*.java")
    private String includePattern;

    @Option(names = {"--exclude-pattern"},
            description = "Comma-separated glob patterns to exclude (e.g. **/generated/**)",
            defaultValue = "")
    private String excludePattern;

    @Option(names = {"--verbose", "-v"},
            description = "Verbose output",
            defaultValue = "false")
    private boolean verbose;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        GuardianConfig config = GuardianConfig.builder()
            .projectPath(projectPath.toAbsolutePath())
            .mode(mode)
            .outputDir(outputDir)
            .llmProvider(llmProvider)
            .apiKey(resolveApiKey())
            .ollamaUrl(ollamaUrl)
            .model(resolveModel())
            .minCoverage(minCoverage)
            .failOnMissing(failOnMissing)
            .includePattern(includePattern)
            .excludePattern(excludePattern)
            .verbose(verbose)
            .build();

        Guardian guardian = new Guardian(config);
        return guardian.run();
    }

    private String resolveApiKey() {
        if (!apiKey.isEmpty()) return apiKey;
        switch (llmProvider.toLowerCase()) {
            case "anthropic": return System.getenv().getOrDefault("ANTHROPIC_API_KEY", "");
            case "openai":    return System.getenv().getOrDefault("OPENAI_API_KEY", "");
            default:          return "";
        }
    }

    private String resolveModel() {
        if (!model.isEmpty()) return model;
        switch (llmProvider.toLowerCase()) {
            case "anthropic": return "claude-sonnet-4-20250514";
            case "openai":    return "gpt-4o";
            case "ollama":    return "codellama";
            default:          return "codellama";
        }
    }
}
