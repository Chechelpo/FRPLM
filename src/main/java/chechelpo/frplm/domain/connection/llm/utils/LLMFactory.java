package chechelpo.frplm.domain.connection.llm.utils;

import ch.qos.logback.classic.Logger;
import chechelpo.frplm.domain.connection.llm.LLMBackend;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.types.InvalidKey;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityFactory;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;

@Service
public final class LLMFactory extends EntityFactory<LlmConnectionRecord, LLMConnection, LLMRepository> {
    private final static Logger log = (Logger) LoggerFactory.getLogger(LLMFactory.class);

    LLMFactory(LLMRepository repository) {
        super(repository);
    }
    @Override
    public @NotNull LLMConnection instantiate(@NotNull EntityKey<LlmConnectionRecord> key){
        if (!repository.exists(key)){
            log.error("No LLM connection found for key: {}", key.toString());
            throw new InvalidKey("No LLM connection found", Severity.SYSTEM);
        }

        return switch (LLMBackend.get(repository.get(LLM_CONNECTION.TYPE, key))){
            case LLMBackend.NANOGPT -> new NanoGPT(key, repository);
            case OPENAI_COMPATIBLE -> throw new IllegalArgumentException("OpenAI compatible not yet supported");
            case null -> throw new IllegalArgumentException("LLM connection type is null");
        };
    }
}
