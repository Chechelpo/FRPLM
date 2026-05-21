package chechelpo.frplm.domain.lorebook.entry.utils;

import chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.Entity;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;

import java.util.concurrent.ThreadLocalRandom;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

public final class Entry extends Entity<EntryRecord, EntryRepository> {
    Entry(EntityKey<EntryRecord> key, EntryRepository repository) {
        super(key, repository);
    }

    public String content(){
        return this.get(ENTRY.CONTENT);
    }

    /**
     * @apiNote assumes this entry's keywords/outlets where already matched for
     */
    public boolean activates(byte[][] historyEmbeddings){
        if (!this.get(ENTRY.ENABLED) || this.get(ENTRY.CONTENT) == null) return false;

        ActivationStrategy activationStrategy = ActivationStrategy.of(this.get(ENTRY.STRATEGY));
        return switch (activationStrategy) {
            case CONSTANT -> true;
            case COMMON -> evaluateCommonActivation();
            case EMBEDDING -> evaluateCommonActivation() || evaluateEmbeddingActivation(historyEmbeddings);
        };
    }
    private boolean evaluateCommonActivation(){
        int probability = this.get(ENTRY.PROBABILITY);
        return ThreadLocalRandom.current().nextInt(100) < probability;
    }
    private boolean evaluateEmbeddingActivation(byte[][] historyEmbeddings){
        System.err.println("Embedding activation not yet implemented");
        return false;
    }

}
