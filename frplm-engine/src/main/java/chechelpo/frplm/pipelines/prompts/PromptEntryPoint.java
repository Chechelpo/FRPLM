package chechelpo.frplm.pipelines.prompts;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
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
        PromptRenderContext promptRenderContext = SessionContext.getPromptRenderContext(session, engine);
        PROMPT_LOGGER.debug("PROMPT RENDER CONTEXT: \n {} ", promptRenderContext.toString());

        IntSet lorebookIDs = IntSetFactory.ofValues(Arrays.stream(promptRenderContext.lorebooks())
                .flatMapToInt(record -> IntStream.of(record.getId())).toArray()
        );
        PROMPT_LOGGER.trace("Lorebook IDs: {}", lorebookIDs);

        List<PromptSectionRecord> promptSectionRecords = engine.sections().getOrderedSectionsOfTemplate(promptRenderContext.template());
        PROMPT_LOGGER.trace("Section records: \n {}", promptSectionRecords);
        IntObjectPair<String>[] outlets = engine.outlets().getOutlets(lorebookIDs);
        PROMPT_LOGGER.trace("Outlets : \n {}", Arrays.toString(outlets));
        List<OutletInjection.OutletsOfSections> outletsOfSections = OutletDetection.getReadyToInsert(outlets, promptSectionRecords);
        PROMPT_LOGGER.trace("OutletsOfSections : \n {}", outletsOfSections);

        List<MessagesRecord> chatHistory = engine.messages().getMessages(session);
        IntObjectPair<String>[] keywords = engine.keywords().getKeywords(lorebookIDs);
        PROMPT_LOGGER.trace("Keywords of lorebooks : \n {}", Arrays.toString(keywords));
        PROMPT_LOGGER.trace("OutletsOfSections : \n {}", outletsOfSections);
        IntSet detectedKeywords = KeywordDetection.detectedKeywords(keywords, chatHistory);
        PROMPT_LOGGER.trace("Detected keywords : \n {}", detectedKeywords);
        Map<Integer, List<EntryRecord>> entriesByOutlet = engine.entries().getByOutletsWith(lorebookIDs, detectedKeywords);
        PROMPT_LOGGER.trace("Entries by outlets : \n {}", entriesByOutlet);

        List<ChatCompletionMessage> renderedMessages = OutletInjection.injectAndCreateRequest(outletsOfSections, entriesByOutlet, chatHistory);
        String modelID = engine.llm().fromTemplate(promptRenderContext.template())
                    .orElseThrow(() -> new NotInitialized("Prompt has no connection assigned", Severity.USER))
                    .getModel();

        return new ChatCompletionRequest(
                modelID,
                renderedMessages,
                ChatCompletionFactory.parametersFrom(promptRenderContext.template()),
                ChatCompletionFactory.configFrom(promptRenderContext.template()),
                null
        );
    }



}
