package io.github.chechelpo.frplm.domain.connection.llm;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.utils.stable_records.StableRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.API_HOSTS;
import static org.jooq.impl.DSL.max;

/**
 * They are all assumed to be chat-completions.
 */
public enum LLMBackend implements StableRecord<ApiHostsRecord> {
    NANO_GPT(
            0,
            "NanoGPT",
            "https://nano-gpt.com",
            createJsonClient("https://nano-gpt.com", 4 * 1024 * 1024)
    ),
    OPEN_ROUTER(
            1,
            "Open router",
            "https://openrouter.ai",
            createJsonClient("https://openrouter.ai", 4*1024*1024)
    ),
    OPENAI_COMPATIBLE(null, "OpenAI Compatible", null, null),
    ;

    private static final Int2ObjectArrayMap<LLMBackend> STABLE_IDS =
            new Int2ObjectArrayMap<>(LLMBackend.values().length);


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
     * Stable id for services like anthropic, nanoGPT or OpenAI.
     */
    public final @Nullable Integer stable_id;

    /**
     * Display name.
     */
    public final String name;

    /**
     * Host, may be null for custom or dynamic providers.
     */
    public final @Nullable URI host;

    private final @Nullable WebClient client;

    LLMBackend(
            @Nullable Integer stable_id,
            String name,
            @Nullable String host,
            @Nullable WebClient client
    ) {
        this.name = name;
        this.stable_id = stable_id;
        this.client = client;
        this.host = host == null ? null : URI.create(host);
    }

    private static WebClient createJsonClient(String baseUrl, int maxInMemoryBytes) {
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(maxInMemoryBytes)
                )
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public static int[] getIDs() {
        return STABLE_IDS.keySet().toIntArray();
    }

    @Contract(pure = true)
    public static @Nullable LLMBackend get(int id) {
        return STABLE_IDS.get(id);
    }

    public static boolean isStandardBackend(int id) {
        return STABLE_IDS.containsKey(id);
    }

    @Override
    public void runCustomConfig(DSLContext dslContext) {
        Integer nextId = dslContext.select(max(API_HOSTS.ID).plus(1))
                .from(API_HOSTS)
                .fetchOneInto(Integer.class);

        if (nextId == null) {
            nextId = 1;
        }

        dslContext.execute("ALTER TABLE API_HOSTS ALTER COLUMN ID RESTART WITH " + nextId);
    }

    @Override
    public Table<ApiHostsRecord> getTable() {
        return API_HOSTS;
    }

    @Override
    @Contract(pure = true)
    public Optional<EntityDataPayload<ApiHostsRecord>> toPayload() {
        if (host == null || stable_id == null) return Optional.empty();

        return Optional.of(
                EntityDataPayload.<ApiHostsRecord>builder()
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
    public @NotNull Optional<WebClient> getDefaultClient() {
        return Optional.ofNullable(this.client);
    }

}