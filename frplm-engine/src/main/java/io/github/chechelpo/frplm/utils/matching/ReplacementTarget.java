package io.github.chechelpo.frplm.utils.matching;

public interface ReplacementTarget {

    ReplacementResult replaceAt(
            String content,
            String replacement
    );

    record ReplacementResult(
            String newContent,
            boolean replaced
    ) {}
}