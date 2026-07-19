package io.github.chechelpo.frplm.utils.tokenizers;

@FunctionalInterface
public interface Tokenizer {
    int tokenCount(String text);
}
