package com.junitguardian.scanner;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.junitguardian.GuardianConfig;
import com.junitguardian.model.SourceFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Discovers Java source files in src/main/java (or project root),
 * excluding test files and generated code.
 */
public class JavaSourceScanner {

    private final GuardianConfig config;

    public JavaSourceScanner(GuardianConfig config) {
        this.config = config;
    }

    public List<SourceFile> scanSources() throws IOException {
        List<SourceFile> result = new ArrayList<>();

        // Prefer src/main/java if it exists (Maven/Gradle layout)
        Path srcMain = config.getProjectPath().resolve("src/main/java");
        Path scanRoot = Files.isDirectory(srcMain) ? srcMain : config.getProjectPath();

        PathMatcher excludeMatcher = buildExcludeMatcher();

        try (Stream<Path> walk = Files.walk(scanRoot)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !isTestFile(p))
                .filter(p -> !excludeMatcher.matches(config.getProjectPath().relativize(p)))
                .forEach(p -> {
                    try {
                        SourceFile sf = parseSourceFile(p, scanRoot);
                        if (sf != null) result.add(sf);
                    } catch (IOException e) {
                        System.err.println("  Warning: could not read " + p + ": " + e.getMessage());
                    }
                });
        }
        return result;
    }

    private boolean isTestFile(Path p) {
        String s = p.toString().replace('\\', '/');
        // skip anything in src/test or with a "Test" suffix already
        return s.contains("/src/test/") || s.contains("/test/") || 
               p.getFileName().toString().endsWith("Test.java") ||
               p.getFileName().toString().endsWith("Tests.java") ||
               p.getFileName().toString().endsWith("IT.java");
    }

    private PathMatcher buildExcludeMatcher() {
        String pattern = config.getExcludePattern();
        if (pattern == null || pattern.isBlank()) {
            return path -> false; // nothing excluded
        }
        // Support comma-separated list of globs
        List<String> patterns = Arrays.asList(pattern.split(","));
        FileSystem fs = FileSystems.getDefault();
        List<PathMatcher> matchers = patterns.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> fs.getPathMatcher("glob:" + s))
            .toList();
        return path -> matchers.stream().anyMatch(m -> m.matches(path));
    }

    private SourceFile parseSourceFile(Path absolutePath, Path scanRoot) throws IOException {
        String source = Files.readString(absolutePath);
        Path relative = config.getProjectPath().relativize(absolutePath);

        String packageName = "";
        String className   = absolutePath.getFileName().toString().replace(".java", "");

        try {
            CompilationUnit cu = StaticJavaParser.parse(absolutePath);
            packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");
            // Use the primary public class name if available
            className = cu.findFirst(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse(className);
        } catch (Exception e) {
            // Parser failed (e.g. newer Java syntax) — use file name fallback
        }

        return new SourceFile(absolutePath, relative, packageName, className, source);
    }
}
