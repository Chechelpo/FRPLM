package chechelpo.frplm.config.logging;

import ch.qos.logback.classic.Level;
import org.jetbrains.annotations.NotNull;

enum LoggerConfig {
    EXCEPTION_LOGGING(Logger_names.EXCEPTIONS,Level.TRACE,false);

    private final Logger_names logger_name;
    private final Level logger_level;

    LoggerConfig(Logger_names logger_name, Level logger_level, boolean save_to_disk) {
        this.logger_name = logger_name;
        this.logger_level = logger_level;
    }

    public Level getLoggerLevel() {
        return logger_level;
    }
    Logger_names getLoggerName() {
        return logger_name;
    }

    public static @NotNull LoggerConfig getConfigForClass(Logger_names name) {
        for (LoggerConfig config : LoggerConfig.values()) {
            if (config.getLoggerName().equals(name)) {
                return config;
            }
        }
        //Should be unreachable
        throw new IllegalArgumentException("No logger config found for " + name);
    }
}
