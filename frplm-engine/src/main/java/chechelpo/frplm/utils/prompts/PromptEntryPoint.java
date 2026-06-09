package chechelpo.frplm.utils.prompts;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.NotInitialized;
import chechelpo.frplm.openai_compatible.ChatCompletionMessage;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.pipelines.FullEngineContext;
import chechelpo.frplm.utils.dto.ChatCompletionFactory;
import chechelpo.frplm.jooq.generated.tables.records.*;
import chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.*;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class PromptEntryPoint {
    private PromptEntryPoint() {}
    static final Logger PROMPT_LOGGER = (Logger) LoggerFactory.getLogger("PROMPT_ENGINE");
    static{
        PROMPT_LOGGER.setLevel(Level.TRACE);
    }

    @CheckReturnValue
    public static @NotNull ChatCompletionRequest makePromptFor (
            @NotNull SessionsRecord session,
            @NotNull FullEngineContext engine
    ){

        return null;
    }



}
