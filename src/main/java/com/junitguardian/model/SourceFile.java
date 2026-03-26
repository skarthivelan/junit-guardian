package com.junitguardian.model;

import java.nio.file.Path;

/**
 * Represents a discovered Java source file.
 */
public class SourceFile {
    private final Path absolutePath;
    private final Path relativePath;
    private final String packageName;
    private final String className;
    private final String sourceCode;

    public SourceFile(Path absolutePath, Path relativePath,
                      String packageName, String className, String sourceCode) {
        this.absolutePath = absolutePath;
        this.relativePath = relativePath;
        this.packageName  = packageName;
        this.className    = className;
        this.sourceCode   = sourceCode;
    }

    public Path   getAbsolutePath() { return absolutePath; }
    public Path   getRelativePath() { return relativePath; }
    public String getPackageName()  { return packageName; }
    public String getClassName()    { return className; }
    public String getSourceCode()   { return sourceCode; }
    public String getFileName()     { return absolutePath.getFileName().toString(); }

    @Override public String toString() { return relativePath.toString(); }
}
