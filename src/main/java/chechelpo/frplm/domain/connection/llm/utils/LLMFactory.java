package chechelpo.frplm.domain.connection.llm.utils;

import ch.qos.logback.classic.Logger;
import chechelpo.frplm.domain.connection.llm.LLMBackend;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.types.InvalidID;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;

@Service
public final class LLMFactory {
    private final static Logger log = (Logger) LoggerFactory.getLogger(LLMFactory.class);
    private final LLMRepository repository;

    LLMFactory(LLMRepository repository) {
        this.repository = repository;
    }

    public @NotNull LLMConnection getOf(EntityKey<LlmConnectionRecord> key){
        if (!repository.exists(key)){
            log.error("No LLM connection found for key: {}", key.toString());
            throw new InvalidID("No LLM connection found", Severity.SYSTEM);
        }

        return switch (LLMBackend.get(repository.getFromLLM(LLM_CONNECTION.TYPE, key))){
            case LLMBackend.NANOGPT -> new NanoGPT(repository, key);
            case OPENAI_COMPATIBLE -> throw new IllegalArgumentException("OpenAI compatible not yet supported");
            case null -> throw new IllegalArgumentException("LLM connection type is null");
        };
    }
}
