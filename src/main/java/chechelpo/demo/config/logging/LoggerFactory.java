package chechelpo.demo.config.logging;

import ch.qos.logback.classic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public final class LoggerFactory {
    private LoggerFactory() {}
    private static final EnumSet<Logger_names> REGISTERED_LOGGERS = EnumSet.noneOf(Logger_names.class);

    public static @NotNull Logger get_logger(Logger_names logger_name){
        if (REGISTERED_LOGGERS.contains(logger_name))
            throw new IllegalStateException("Logger already registered during initialization: " + logger_name);

        REGISTERED_LOGGERS.add(logger_name);

        LoggerConfig config = LoggerConfig.getConfigForClass(logger_name);
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(logger_name.toString());
        logger.setLevel(config.getLoggerLevel());

        return logger;
    }
}
