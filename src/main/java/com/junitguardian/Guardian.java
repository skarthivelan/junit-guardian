package com.junitguardian;

import com.junitguardian.llm.LlmClient;
import com.junitguardian.llm.LlmClientFactory;
import com.junitguardian.model.ScanReport;
import com.junitguardian.model.SourceFile;
import com.junitguardian.scanner.JavaSourceScanner;
import com.junitguardian.scanner.TestCoverageAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Main orchestrator: scans source, checks test coverage, and optionally
 * calls the LLM to generate missing JUnit tests.
 */
public class Guardian {

    private final GuardianConfig config;
    private final ConsoleLogger log;

    public Guardian(GuardianConfig config) {
        this.config = config;
        this.log    = new ConsoleLogger(config.isVerbose());
    }

    /** @return 0 = success/pass, 1 = coverage below threshold */
    public int run() throws Exception {
        log.banner("JUnit Guardian v1.0.0");
        log.info("Project  : " + config.getProjectPath());
        log.info("Mode     : " + config.getMode());
        log.info("Provider : " + config.getLlmProvider() + " / " + config.getModel());
        log.info("Min cov  : " + config.getMinCoverage() + "%");
        System.out.println();

        // 1. Discover all Java source files
        JavaSourceScanner scanner = new JavaSourceScanner(config);
        List<SourceFile> sources = scanner.scanSources();
        log.info("Found " + sources.size() + " Java source file(s).");

        if (sources.isEmpty()) {
            log.warn("No Java source files found. Check --include-pattern.");
            return config.isFailOnMissing() ? 1 : 0;
        }

        // 2. Analyze existing test coverage
        TestCoverageAnalyzer analyzer = new TestCoverageAnalyzer(config);
        ScanReport report = analyzer.analyze(sources);

        printCoverageReport(report);

        boolean belowThreshold = report.getCoveragePercent() < config.getMinCoverage();

        // 3. Handle modes
        switch (config.getMode().toLowerCase()) {
            case "scan":
                break; // just report

            case "generate":
                if (!report.getUncoveredFiles().isEmpty()) {
                    generateTests(report.getUncoveredFiles());
                } else {
                    log.success("All source classes have corresponding test files!");
                }
                break;

            case "report":
                generateTests(report.getUncoveredFiles());
                exportReport(report);
                break;

            default:
                log.error("Unknown mode: " + config.getMode() + ". Use: scan | generate | report");
                return 1;
        }

        if (belowThreshold) {
            if (config.isFailOnMissing()) {
                log.error(String.format(
                    "Coverage %.1f%% is below required minimum %d%%. Commit blocked.",
                    report.getCoveragePercent(), config.getMinCoverage()));
                return 1;
            } else {
                log.warn(String.format(
                    "Coverage %.1f%% is below the recommended minimum of %d%%. " +
                    "Commit allowed (warn-only mode).",
                    report.getCoveragePercent(), config.getMinCoverage()));
                return 0;
            }
        }

        log.success(String.format("Coverage %.1f%% meets the required minimum %d%%.",
            report.getCoveragePercent(), config.getMinCoverage()));
        return 0;
    }

    private void generateTests(List<SourceFile> uncovered) throws Exception {
        if (uncovered.isEmpty()) {
            log.info("No uncovered files to generate tests for.");
            return;
        }

        log.info("\nGenerating JUnit tests for " + uncovered.size() + " file(s) using LLM...");

        LlmClient llm = LlmClientFactory.create(config);

        for (SourceFile sf : uncovered) {
            log.info("  → Generating: " + sf.getRelativePath());
            String testCode = llm.generateTests(sf);

            if (testCode == null || testCode.isBlank()) {
                log.warn("    LLM returned empty response for " + sf.getFileName());
                continue;
            }

            Path outputPath = resolveOutputPath(sf);
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, testCode);
            log.success("    ✓ Written: " + outputPath);
        }
    }

    private Path resolveOutputPath(SourceFile sf) {
        // e.g. src/main/java/com/example/Foo.java → src/test/java/com/example/FooTest.java
        String testFileName = sf.getFileName().replace(".java", "Test.java");
        String pkg = sf.getPackageName().replace(".", "/");
        return config.getProjectPath()
            .resolve(config.getOutputDir())
            .resolve(pkg)
            .resolve(testFileName);
    }

    private void printCoverageReport(ScanReport report) {
        System.out.println("┌──────────────────────────────────────────┐");
        System.out.printf( "│  Coverage: %5.1f%%  (%d / %d classes)%s│%n",
            report.getCoveragePercent(),
            report.getCoveredCount(),
            report.getTotalCount(),
            " ".repeat(Math.max(0, 12 - String.valueOf(report.getTotalCount()).length())));
        System.out.println("└──────────────────────────────────────────┘");

        if (!report.getUncoveredFiles().isEmpty()) {
            log.warn("Classes missing tests:");
            report.getUncoveredFiles().forEach(f ->
                System.out.println("  ✗ " + f.getRelativePath()));
        }
        System.out.println();
    }

    private void exportReport(ScanReport report) throws IOException {
        Path out = config.getProjectPath().resolve("junit-guardian-report.json");
        Files.writeString(out, report.toJson());
        log.info("Report written: " + out);
    }
}
