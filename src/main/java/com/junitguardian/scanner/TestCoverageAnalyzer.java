package com.junitguardian.scanner;

import com.junitguardian.GuardianConfig;
import com.junitguardian.model.ScanReport;
import com.junitguardian.model.SourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Checks whether each source class has a corresponding JUnit test file.
 * Strategy: looks for ClassNameTest.java (or ClassNameTests.java / ClassNameIT.java)
 * anywhere under src/test/java.
 */
public class TestCoverageAnalyzer {

    private final GuardianConfig config;

    public TestCoverageAnalyzer(GuardianConfig config) {
        this.config = config;
    }

    public ScanReport analyze(List<SourceFile> sources) throws IOException {
        Set<String> testClassNames = collectTestClassNames();

        List<SourceFile> covered   = new ArrayList<>();
        List<SourceFile> uncovered = new ArrayList<>();

        for (SourceFile sf : sources) {
            boolean hasCoverage = testClassNames.contains(sf.getClassName() + "Test")
                || testClassNames.contains(sf.getClassName() + "Tests")
                || testClassNames.contains(sf.getClassName() + "IT");

            if (hasCoverage) covered.add(sf);
            else             uncovered.add(sf);
        }

        return new ScanReport(sources.size(), covered.size(), uncovered);
    }

    /** Collect all class names found in src/test/java */
    private Set<String> collectTestClassNames() throws IOException {
        Set<String> names = new HashSet<>();
        Path testRoot = config.getProjectPath().resolve("src/test/java");

        if (!Files.isDirectory(testRoot)) return names;

        try (Stream<Path> walk = Files.walk(testRoot)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    String name = p.getFileName().toString().replace(".java", "");
                    names.add(name);
                });
        }
        return names;
    }
}
