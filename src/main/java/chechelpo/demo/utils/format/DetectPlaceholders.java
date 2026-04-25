package chechelpo.demo.utils.format;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DetectPlaceholders {
    private DetectPlaceholders() {}
    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{[^}]+}}");

    /**
     * Detects whether the input string contains at least one {{placeholder}}.
     *
     * @param text the text to scan
     * @return true if any placeholder is detected, false otherwise
     */
    public static boolean hasAny(String text) {
        if (text == null) return false;

        Matcher m = PLACEHOLDER_PATTERN.matcher(text);
        return m.find();
    }

    /**
     * Detects {{placeholder}} tokens in a text which are not included in the allowed list.
     * @param text to scan
     * @param allowed a list with allowed placeholders
     * @return at the first occurrence of a placeholder that's not in the list.
     */
    public static boolean isValid(@NotNull String text,
                                  @Nullable Set<String> allowed
    ){
        if (allowed == null || allowed.isEmpty()) return !hasAny(text);

        Matcher m = PLACEHOLDER_PATTERN.matcher(text);

        while (m.find()) {
            if (!allowed.contains(m.group())) {
                return false;
            }
        }

        return true;
    }
}
