package chechelpo.frplm.domain.lorebook.utils;

import chechelpo.frplm.domain.lorebook.entry.utils.Entry;
import chechelpo.frplm.domain.prompts.section.utils.DetectedOutlet;
import chechelpo.frplm.domain.prompts.template.utils.Prompt;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.Entity;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class Lorebook extends Entity<LorebooksRecord, LorebookRepository> {
    Lorebook(EntityKey<LorebooksRecord> key, LorebookRepository repository) {
        super(key, repository);
    }

    @Contract(mutates = "param1")
    public void injectActivatedEntries(
            @NotNull Prompt prompt,
            @NotNull List<DetectedOutlet> outletAnchor,
            @NotNull IntSet detectedKeywords
    ){
        for (DetectedOutlet injectionAnchor : outletAnchor) {
            int outletID = injectionAnchor.outletID();
            Entry[] entries = getWithOutletAndKeywords(outletID, detectedKeywords);

            for (Entry entry : entries) {
                if (entry.activates(null))
                    return;
            }
        }
    }

    private Entry @NotNull [] getWithOutletAndKeywords(int outletID, IntSet keywords) {
        return repository.getEntriesWithOutletAndKeywords(this.key, outletID, keywords);
    }

    public IntSet getKeywords() {
        return repository.getKeywordsIDsOfLorebook(this.key);
    }
}
