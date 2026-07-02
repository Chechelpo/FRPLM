package chechelpo.frplm.utils.tokenizers;

import com.knuddels.jtokkit.api.EncodingType;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Component
public final class TokenizerRegistry {

    private static final String RESOURCE_ROOT = "tokenizers/";

    private final Map<String, Supplier<Tokenizer>> factories;
    private final ConcurrentMap<String, Tokenizer> loadedTokenizers =
            new ConcurrentHashMap<>();

    public TokenizerRegistry() {
        this.factories = Map.ofEntries(
                // SentencePiece tokenizers
                Map.entry(
                        "llama",
                        () -> sentencePiece("llama.model")
                ),
                Map.entry(
                        "mistral",
                        () -> sentencePiece("mistral.model")
                ),
                Map.entry(
                        "gemma",
                        () -> sentencePiece("gemma.model")
                ),
                Map.entry(
                        "jamba",
                        () -> sentencePiece("jamba.model")
                ),
                Map.entry(
                        "yi",
                        () -> sentencePiece("yi.model")
                ),
                Map.entry(
                        "nerdstash",
                        () -> sentencePiece("nerdstash.model")
                ),
                Map.entry(
                        "nerdstash_v2",
                        () -> sentencePiece("nerdstash_v2.model")
                ),

                // Hugging Face-compatible tokenizer JSON files
                Map.entry(
                        "claude",
                        () -> huggingFaceJson("claude.json")
                ),
                Map.entry(
                        "llama3",
                        () -> huggingFaceJson("llama3.json")
                ),

                // Standard TikToken encodings from JTokkit
                Map.entry(
                        "cl100k_base",
                        () -> new JTokkitTokenizer(
                                EncodingType.CL100K_BASE
                        )
                ),
                Map.entry(
                        "o200k_base",
                        () -> new JTokkitTokenizer(
                                EncodingType.O200K_BASE
                        )
                ),
                Map.entry(
                        "r50k_base",
                        () -> new JTokkitTokenizer(
                                EncodingType.R50K_BASE
                        )
                )
        );
    }

    public Tokenizer getForModel(String id) {
        String tokenizerId = resolveTokenizerId(id);

        return loadedTokenizers.computeIfAbsent(
                tokenizerId,
                ignored -> createTokenizer(tokenizerId)
        );
    }

    private Tokenizer createTokenizer(String tokenizerId) {
        Supplier<Tokenizer> factory = factories.get(tokenizerId);

        if (factory == null) {
            throw new IllegalArgumentException(
                    "No tokenizer factory registered for: "
                            + tokenizerId
            );
        }

        return factory.get();
    }

    private static Tokenizer sentencePiece(String filename) {
        return new SentencePieceTokenizer(
                RESOURCE_ROOT + filename
        );
    }

    private static Tokenizer huggingFaceJson(String filename) {
        return new HuggingFaceJsonTokenizer(
                RESOURCE_ROOT + filename
        );
    }

    private static String resolveTokenizerId(String modelId) {
        Objects.requireNonNull(modelId, "modelId must not be null");

        String model = modelId
                .trim()
                .toLowerCase(Locale.ROOT);

        if (model.isEmpty()) {
            throw new IllegalArgumentException(
                    "modelId must not be blank"
            );
        }

        /*
         * More-specific names must be checked before broad names.
         */

        if (containsAny(model, "nerdstash_v2", "nerdstash-v2")) {
            return "nerdstash_v2";
        }

        if (model.contains("nerdstash")) {
            return "nerdstash";
        }

        if (containsAny(model, "llama3", "llama-3")) {
            return "llama3";
        }

        if (model.contains("llama")) {
            return "llama";
        }

        if (model.contains("mistral")) {
            return "mistral";
        }

        if (model.contains("jamba")) {
            return "jamba";
        }

        if (model.contains("yi")) {
            return "yi";
        }

        if (containsAny(model, "gemma", "gemini", "learnlm")) {
            return "gemma";
        }

        if (model.contains("claude")) {
            return "claude";
        }

        /*
         * Newer OpenAI-family models generally use the
         * o200k_base vocabulary family.
         */
        if (containsAny(
                model,
                "gpt-5",
                "gpt-4o",
                "gpt-4.1",
                "gpt-4.5",
                "o1",
                "o3",
                "o4"
        )) {
            return "o200k_base";
        }

        /*
         * GPT-3.5 and older GPT-4-family models.
         */
        if (containsAny(
                model,
                "gpt-4",
                "gpt-3.5",
                "text-embedding-ada-002"
        )) {
            return "cl100k_base";
        }

        if (containsAny(
                model,
                "gpt2",
                "gpt-2",
                "text-davinci"
        )) {
            return "r50k_base";
        }

        throw new IllegalArgumentException(
                "No tokenizer mapping exists for model: " + modelId
        );
    }

    private static boolean containsAny(
            String value,
            String... fragments
    ) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }

        return false;
    }

    @PreDestroy
    public void closeLoadedTokenizers() {
        for (Tokenizer tokenizer : loadedTokenizers.values()) {
            if (tokenizer instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                    // Application is already shutting down.
                }
            }
        }

        loadedTokenizers.clear();
    }
}