package io.github.chechelpo.frplm.utils.matching;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public final class FlexiblePattern implements ReplacementTarget {

    private static final Pattern REGEX_SYNTAX = Pattern.compile(
            """
            \\\\[AbBdDsSwWZzGQEpP]  | # Regex escape/classes
            \\\\.                  | # Explicit escaped character
            \\[.*?]                | # Character class
            \\(.*?\\)              | # Group
            \\^                    | # Start anchor
            \\$                    | # End anchor
            \\.\\*                 | # .*
            \\.\\+                 | # .+
            [*+?]                  | # Quantifier
            \\{\\d+(?:,\\d*)?}     | # {n}, {n,m}
            \\|
            """,
            Pattern.COMMENTS
    );

    private final String source;
    private final boolean regex;
    private final Pattern pattern;

    public FlexiblePattern(String source) {
        this.source = requireValid(source);
        this.regex = looksLikeRegex(source);
        this.pattern = compile(source, regex);
    }

    private static String requireValid(String source) {
        Objects.requireNonNull(source, "Pattern cannot be null");

        if (source.isBlank()) {
            throw new IllegalArgumentException("Pattern cannot be blank");
        }

        return source;
    }
    @Override
    public ReplacementResult replaceAt(String content, String toInject) {
        Objects.requireNonNull(content, "Content cannot be null");

        if (toInject == null || toInject.isBlank()) {
            return new ReplacementResult(content, false);
        }

        Matcher matcher = pattern.matcher(content);

        if (!matcher.find()) {
            return new ReplacementResult(content, false);
        }

        String replaced = matcher.replaceAll(
                Matcher.quoteReplacement(toInject)
        );

        return new ReplacementResult(replaced, true);
    }

    @Contract("_ -> new")
    public static @NotNull FlexiblePattern of(String source) {
        return new FlexiblePattern(source);
    }

    private static boolean looksLikeRegex(String source) {
        if (!REGEX_SYNTAX.matcher(source).find()) {
            return false;
        }

        try {
            Pattern.compile(source);
            return true;
        } catch (PatternSyntaxException ignored) {
            return false;
        }
    }

    private static Pattern compile(String source, boolean regex) {
        return regex
                ? compileRegex(source)
                : compileKeyword(source);
    }

    private static Pattern compileRegex(String regex) {
        return Pattern.compile(
                regex,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
    }

    /**
     * Converts a normal keyword into a case-insensitive,
     * punctuation-insensitive search pattern.
     *
     * "red dragon"
     *
     * matches:
     *   red dragon
     *   red-dragon
     *   red.dragon
     *   RED, DRAGON
     */
    private static Pattern compileKeyword(String keyword) {
        String body = Arrays.stream(
                        keyword.trim().split("[^\\p{L}\\p{N}]+")
                )
                .filter(token -> !token.isEmpty())
                .map(Pattern::quote)
                .collect(
                        Collectors.joining("[^\\p{L}\\p{N}]+")
                );

        if (body.isEmpty()) {
            throw new IllegalArgumentException(
                    "Keyword must contain at least one letter or number"
            );
        }

        return Pattern.compile(
                "(?<![\\p{L}\\p{N}])"
                        + body
                        + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
    }

    public boolean matches(String content) {
        return pattern.matcher(
                Objects.requireNonNull(content)
        ).matches();
    }

    public boolean findIn(String content) {
        return pattern.matcher(
                Objects.requireNonNull(content)
        ).find();
    }

    public Pattern asPattern() {
        return pattern;
    }

    public String getSource() {
        return source;
    }

    public boolean isRegex() {
        return regex;
    }

    @Override
    public String toString() {
        return source;
    }
}