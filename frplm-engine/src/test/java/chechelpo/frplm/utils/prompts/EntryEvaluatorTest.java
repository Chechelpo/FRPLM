package chechelpo.frplm.utils.prompts;

import chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntryEvaluatorTest {
    @Test
    void activates_constantEntryAlwaysActive() {
        EntryRecord record = new EntryRecord();
        record.setStrategy(ActivationStrategy.CONSTANT.stable_id);
        assertTrue(EntryEvaluator.activates(record, 1000, 2000));
    }

    @Test
    void constant_disregardsCommonActivationParameters(){
        EntryRecord record = new EntryRecord();
        record.setStrategy(ActivationStrategy.CONSTANT.stable_id);
        record.setScanDepth((short) 4);
        record.setNonRecursable(false);

        assertTrue(EntryEvaluator.activates(record, 1000, 2));
        assertTrue(EntryEvaluator.activates(record, 1000, 5));
    }

    @Test
    void activates_reactsOnScanDepth() {
        EntryRecord record = new EntryRecord();
        record.setStrategy(ActivationStrategy.COMMON.stable_id);
        record.setScanDepth((short) 4);
        record.setNonRecursable(false);

        assertTrue(EntryEvaluator.activates(record, 1000, 2));
        assertFalse(EntryEvaluator.activates(record, 1000, 5));
    }

    @Test
    void activates_reactsOnRecursionStep(){
        EntryRecord record = new EntryRecord();
        record.setStrategy(ActivationStrategy.COMMON.stable_id);
        record.setNonRecursable(true);

        assertFalse(EntryEvaluator.activates(record, 2, 2));
        assertFalse(EntryEvaluator.activates(record, 1000, 5));
        assertTrue(EntryEvaluator.activates(record, 0, 5));
    }
}