package chechelpo.frplm.domain.connection.llm;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LLMBackendTest {

    @Test
    void uniqueValues(){
        int backendAmount = LLMBackend.values().length;
        Set<Integer> uniqueIds = new IntArraySet(backendAmount);
        Set<String> names = new HashSet<>(backendAmount);

        for (LLMBackend backend : LLMBackend.values()) {
            if (backend.stable_id != null){
                assertFalse(uniqueIds.contains(backend.stable_id), "Duplicate stable ID: " + backend.stable_id);
                uniqueIds.add(backend.stable_id);

                assertNotNull(backend.host, "Host of a standard backend is null: " + backend);
                assertFalse(names.contains(backend.host.toString()), "Duplicate URL: " + backend.stable_id);
                names.add(backend.host.toString());
            }
        }
    }


    @Test
    void dynamicBackendsDoNotExposeStableKeyOrPayload() {
        for (LLMBackend backend : LLMBackend.values()) {
            if (backend.stable_id != null) {
                continue;
            }

            assertAll(
                    "dynamic backend: " + backend,
                    () -> assertFalse(
                            backend.toKey().isPresent(),
                            "Dynamic backend unexpectedly has entity key: " + backend
                    ),
                    () -> assertFalse(
                            backend.toPayload().isPresent(),
                            "Dynamic backend unexpectedly has entity payload: " + backend
                    )
            );
        }

    }
    @Test
    void unknownIdReturnsOpenAICompatibleFallback() {
        assertSame(LLMBackend.OPENAI_COMPATIBLE, LLMBackend.get(-1));
        assertSame(LLMBackend.OPENAI_COMPATIBLE, LLMBackend.get(Integer.MAX_VALUE));
    }
    @Test
    void getIdsContainsExactlyStandardBackendIds() {
        Set<Integer> expected = Arrays.stream(LLMBackend.values())
                .filter(backend -> backend.stable_id != null)
                .map(backend -> backend.stable_id)
                .collect(Collectors.toSet());

        Set<Integer> actual = Arrays.stream(LLMBackend.getIDs())
                .boxed()
                .collect(Collectors.toSet());

        assertEquals(expected, actual);
    }

    @Test
    void isStandardBackendAgreesWithStableIds() {
        for (LLMBackend backend : LLMBackend.values()) {
            if (backend.stable_id == null) {
                continue;
            }

            assertTrue(
                    LLMBackend.isStandardBackend(backend.stable_id),
                    "Backend should be standard: " + backend
            );
        }

        assertFalse(LLMBackend.isStandardBackend(-1));
        assertFalse(LLMBackend.isStandardBackend(Integer.MAX_VALUE));
    }
}