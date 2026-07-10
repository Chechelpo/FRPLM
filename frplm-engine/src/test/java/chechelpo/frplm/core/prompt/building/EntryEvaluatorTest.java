package chechelpo.frplm.core.prompt.building;

import chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntryEvaluatorTest {
    /*
    @Test
    void entryActivates_constantEntryAlwaysActive() {
        EntryRecord record = new EntryRecord();
        record.setStrategy(ActivationStrategy.CONSTANT.stable_id);
        assertEquals(
                EntryEvaluator.EntryActivation.SUCCESS,
                EntryEvaluator.entryActivates(record, null,1000, 2000, null)
        );
    }

    @Test
    void constant_disregardsCommonActivationParameters(){
        EntryRecord record = new EntryRecord();
        record.setStrategy(ActivationStrategy.CONSTANT.stable_id);
        record.setScanDepth((short) 4);
        record.setNonRecursable(false);

        assertEquals(EntryEvaluator.EntryActivation.SUCCESS,
                EntryEvaluator.entryActivates(record, null, 1000, 2, null)
        );
        assertEquals(
                EntryEvaluator.EntryActivation.SUCCESS,
                EntryEvaluator.entryActivates(record, null,1000, 5, null)
        );
    }

    @Test
    void entryActivates_reactsOnScanDepth() {
        EntryRecord record = new EntryRecord();
        record.setStrategy(ActivationStrategy.COMMON.stable_id);
        record.setScanDepth((short) 4);
        record.setNonRecursable(false);

        assertEquals(
                EntryEvaluator.EntryActivation.SUCCESS,
                EntryEvaluator.entryActivates(record, 1000, 2));
        assertFalse(EntryEvaluator.entryActivates(record, 1000, 5));
    }

    @Test
    void entryActivates_reactsOnRecursionStep(){
        EntryRecord record = new EntryRecord();
        record.setStrategy(ActivationStrategy.COMMON.stable_id);
        record.setNonRecursable(true);

        assertFalse(EntryEvaluator.entryActivates(record, 2, 2));
        assertFalse(EntryEvaluator.entryActivates(record, 1000, 5));
        assertTrue(EntryEvaluator.entryActivates(record, 0, 5));
    }*/
}