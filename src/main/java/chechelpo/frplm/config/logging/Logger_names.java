package chechelpo.frplm.config.logging;

import org.jetbrains.annotations.NotNull;

public enum Logger_names {
    EXCEPTIONS("exceptions"),
    ;
    private final String name;
    Logger_names(@NotNull String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
