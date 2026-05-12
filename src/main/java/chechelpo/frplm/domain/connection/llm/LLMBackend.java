package chechelpo.frplm.domain.connection.llm;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

/**
 * They are all assumed to be chat-completions.
 */
public enum LLMBackend {
    NANOGPT(0, 0,"NanoGPT", "https://nano-gpt.com"),
    OPENAI_COMPATIBLE(null,1, "OpenAI Compatible", null),
    ;

    private static final Int2ObjectArrayMap<LLMBackend> STABLE_IDS = new Int2ObjectArrayMap<>(LLMBackend.values().length);
    static {
        for (LLMBackend backend : LLMBackend.values()) {
            if (backend.stable_id == null) continue;
            int stableId = backend.stable_id;

            if (STABLE_IDS.containsKey(stableId)) {
                throw new IllegalStateException(
                        "Two different LLMBackends subscribe to the same stable id: " + stableId
                );
            }

            STABLE_IDS.put(stableId, backend);
        }
    }

    /** Stable id for services like anthropic, nanoGPT or OpenAI*/
    public final @Nullable Integer stable_id;
    /** Type of this endpoint. Refers to the LLM connection that will later be used */
    public final int type_id;
    /** Display name */
    public final String name;
    /** Host, may be null for custom or dynamic providers */
    public final @Nullable URI host;

    LLMBackend(@Nullable Integer stable_id, int type_id ,String name, @Nullable String host){
        this.name = name;
        this.stable_id =stable_id;
        this.type_id = type_id;

        this.host = host == null ? null : URI.create(host);
    }


    public static int[] getIDs(){
        return STABLE_IDS.keySet().toIntArray();
    }
    public static LLMBackend get(int id){
        return STABLE_IDS.containsKey(id) ? STABLE_IDS.get(id) : OPENAI_COMPATIBLE;
    }
}
