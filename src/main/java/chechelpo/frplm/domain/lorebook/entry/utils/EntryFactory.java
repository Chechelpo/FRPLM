package chechelpo.frplm.domain.lorebook.entry.utils;

import chechelpo.frplm.annotations.Factory;
import chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityFactory;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Factory
public final class EntryFactory extends EntityFactory<EntryRecord, Entry, EntryRepository> {
    EntryFactory(EntryRepository repository) {
        super(repository);
    }

    @Override
    protected Entry instantiate(@NotNull EntityKey<EntryRecord> key) {
        return new Entry(key, repository);
    }

    public Entry @NotNull [] getWithOutlet (
            EntityKey<LorebooksRecord> lorebookKey,
            int outletID,
            IntSet keywordIDs
    ) {
        EntryService entryService = repository.getService();
        List<EntryRecord> records = entryService.getWithOutletAndKeywords(lorebookKey, outletID, keywordIDs);

        Entry[] entries = new Entry[records.size()];
        for (int i = 0; i < records.size(); i++)
            entries[i] = instantiate(entryService.keyOf(records.get(i)));

        return entries;
    }
}
