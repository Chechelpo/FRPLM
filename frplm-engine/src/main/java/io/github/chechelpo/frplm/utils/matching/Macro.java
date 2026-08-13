package io.github.chechelpo.frplm.utils.matching;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public sealed class Macro implements ReplacementTarget permits Outlet {
    public static final Pattern UNRESOLVED_MACRO_LINE = Pattern.compile(
            "(?m)^[ \\t]*\\{\\{\\s*[^{}\\r\\n]+\\s*}}[ \\t]*(?:\\R|$)"
    );
    public static final Pattern UNRESOLVED_MACRO_INLINE = Pattern.compile(
            "\\{\\{\\s*[^{}\\r\\n]+\\s*}}"
    );

    static @NonNull String asMacro(String macroName){
        Objects.requireNonNull(macroName);
        return isMacro(macroName) ? getNormalized(macroName) : "{{" + getNormalized(macroName) + "}}";
    }

    static String getNormalized(String text){
        return text.replaceAll("\\s+", "");
    }

    static boolean isMacro(String text){
        if (text == null) return false;

        String normalized = getNormalized(text);
        return normalized.startsWith("{{") && normalized.endsWith("}}");
    }

    @Contract(pure = true)
    public static @NotNull String stripUnresolvedMacros(@NotNull String content) {
        String withoutMacroOnlyLines = UNRESOLVED_MACRO_LINE
                .matcher(content)
                .replaceAll("");

        return UNRESOLVED_MACRO_INLINE
                .matcher(withoutMacroOnlyLines)
                .replaceAll("");
    }

    private final String name;
    public Macro(String name) {
        this.name = asMacro(Objects.requireNonNull(name));
    }

    public String getName(){
        return name;
    }

    public Pattern asPattern(){
        return Pattern.compile(
                Pattern.quote(this.name),
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
    }

    public ReplacementResult replaceAt(String content, String toInject) {
        Objects.requireNonNull(content);

        if (toInject == null || toInject.isBlank()) {
            return new ReplacementResult(content, false);
        }

        Matcher matcher = this.asPattern().matcher(content);
        StringBuilder output = null;
        String quotedReplacement = Matcher.quoteReplacement(toInject);

        while (matcher.find()) {
            if (output == null) {
                output = new StringBuilder(content.length());
            }

            matcher.appendReplacement(output, quotedReplacement);
        }

        if (output == null) {
            return new ReplacementResult(content, false);
        }

        matcher.appendTail(output);

        return new ReplacementResult(output.toString(), true);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Macro other
                && name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase(java.util.Locale.ROOT).hashCode();
    }
}
