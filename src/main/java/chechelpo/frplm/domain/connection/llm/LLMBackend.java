package chechelpo.frplm.domain.connection.llm;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

public enum LLMBackend {
    NANOGPT(0,"NanoGPT", "https://nano-gpt.com")
    ;

    private static final Int2ObjectArrayMap<LLMBackend> STABLE_IDS = new Int2ObjectArrayMap<>(LLMBackend.values().length);
    static {
        for (LLMBackend backend : LLMBackend.values()) {
            int stableId = backend.id;

            if (STABLE_IDS.containsKey(stableId)) {
                throw new IllegalStateException(
                        "Two different LLMBackends subscribe to the same stable id: " + stableId
                );
            }

            STABLE_IDS.put(stableId, backend);
        }
    }

    public final int id;
    public final String name;
    public final String host;

    LLMBackend(int stable_id, String name, String host){
        this.name = name;
        this.host = host;
        this.id =stable_id;
    }

    public static int[] getIDs(){
        return STABLE_IDS.keySet().toIntArray();
    }
}
