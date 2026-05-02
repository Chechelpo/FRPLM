package chechelpo.frplm.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public final class NameValidator {
    private NameValidator() {}

    private static final Pattern SAFE_NAME =
            Pattern.compile("^[A-Za-z0-9._-]{1,128}$");

    @Contract(pure = true)
    public static @Nullable String is_valid(String name) {
        if (name == null || name.isBlank()) {
            return "Name cannot be null or empty";
        }

        if (!SAFE_NAME.matcher(name).matches()) {
            return "Invalid name " + name + " (only [A–Z a–z 0–9 . _ -] allowed, length 1–255)";
        }

        if (name.equals(".") || name.equals("..")) {
            return "Invalid name: path traversal detected";
        }

        return null;
    }

    public static void validOrThrow(@Nullable String name) throws IllegalArgumentException {
        String message = is_valid(name);
        if (message != null) throw new IllegalArgumentException(message);
    }
}
