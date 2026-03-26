package com.junitguardian.llm;

import com.junitguardian.model.SourceFile;

/**
 * Builds the system + user prompts sent to any LLM.
 */
public class PromptBuilder {

    public static String systemPrompt() {
        return """
            You are an expert Java developer specialising in writing comprehensive JUnit 5 test suites.
            
            When given a Java source file you MUST:
            1. Analyse every public and package-private method.
            2. Write JUnit 5 tests covering:
               - Happy path (normal inputs)
               - Edge cases (null, empty, boundary values, large inputs)
               - Exception / error paths (@Test + assertThrows)
            3. Use Mockito to mock any collaborators / dependencies.
            4. Use AssertJ fluent assertions where possible.
            5. Follow AAA (Arrange / Act / Assert) pattern.
            6. Return ONLY valid Java source code — no markdown, no explanations, no backticks.
            7. The class must be named exactly <OriginalClassName>Test.
            8. Include the correct package declaration.
            9. Aim for at least 80% branch coverage.
            """;
    }

    public static String userPrompt(SourceFile sf) {
        return String.format("""
            Generate a complete JUnit 5 test class for the following Java source file.
            
            File: %s
            Package: %s
            
            Source code:
            ```java
            %s
            ```
            
            Return ONLY the complete Java test class source code. No explanations or markdown.
            """,
            sf.getFileName(),
            sf.getPackageName().isEmpty() ? "(default package)" : sf.getPackageName(),
            sf.getSourceCode());
    }
}
