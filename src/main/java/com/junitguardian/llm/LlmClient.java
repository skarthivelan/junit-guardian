package com.junitguardian.llm;

import com.junitguardian.model.SourceFile;

/**
 * Abstraction over different LLM providers.
 */
public interface LlmClient {
    /**
     * Ask the LLM to generate a complete JUnit 5 test class for the given source file.
     * @return the Java source code of the generated test class
     */
    String generateTests(SourceFile sourceFile) throws Exception;
}
