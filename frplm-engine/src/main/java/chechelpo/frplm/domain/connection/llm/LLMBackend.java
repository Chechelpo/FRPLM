package chechelpo.frplm.domain.connection.llm;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.interfaces.StableRecord;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.API_HOSTS;

/**
 * They are all assumed to be chat-completions.
 */
public enum LLMBackend implements StableRecord<ApiHostsRecord> {
    NANOGPT(0,
            0,
            "NanoGPT",
            "https://nano-gpt.com",
            WebClient.builder()
                    .baseUrl("https://nano-gpt.com")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build()
    ),
    OPENAI_COMPATIBLE(null, 1, "OpenAI Compatible", null, null),
    ;

    private static final Int2ObjectArrayMap<LLMBackend> STABLE_IDS = new Int2ObjectArrayMap<>(LLMBackend.values().length);
    private static final Logger log = LoggerFactory.getLogger(LLMBackend.class);

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

    /**
     * Stable id for services like anthropic, nanoGPT or OpenAI
     */
    public final @Nullable Integer stable_id;
    /**
     * Type of this endpoint. Refers to the LLM connection that will later be used
     */
    public final int type_id;
    /**
     * Display name
     */
    public final String name;
    /**
     * Host, may be null for custom or dynamic providers
     */
    public final @Nullable URI host;

    private final WebClient client;

    LLMBackend(@Nullable Integer stable_id, int type_id, String name, @Nullable String host, WebClient client) {
        this.name = name;
        this.stable_id = stable_id;
        this.type_id = type_id;
        this.client = client;
        this.host = host == null ? null : URI.create(host);
    }


    public static int[] getIDs() {
        return STABLE_IDS.keySet().toIntArray();
    }

    @Contract(pure = true)
    public static @NotNull LLMBackend get(int id) {
        return STABLE_IDS.containsKey(id) ? STABLE_IDS.get(id) : OPENAI_COMPATIBLE;
    }

    @Override
    @Contract(pure = true)
    public Optional<EntityDataPayload<ApiHostsRecord>> toPayload() {
        if (host == null || stable_id == null) return Optional.empty();
        return Optional.of(EntityDataPayload.<ApiHostsRecord>builder()
                .set(API_HOSTS.HOST_URL, this.host.toString())
                .set(API_HOSTS.ID, this.stable_id)
                .build()
        );
    }

    @Override
    @Contract(pure = true)
    public Optional<EntityKey<ApiHostsRecord>> toKey() {
        if (host == null || stable_id == null) return Optional.empty();
        return Optional.of(EntityKey.<ApiHostsRecord>builder()
                .set(API_HOSTS.ID, this.stable_id)
                .build()
        );
    }

    @Contract(pure = true)
    public @NotNull Optional<WebClient> getDefaultClient(){
        return Optional.ofNullable(this.client);
    }
}
