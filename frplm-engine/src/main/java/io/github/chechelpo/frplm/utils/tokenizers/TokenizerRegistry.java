package io.github.chechelpo.frplm.utils.tokenizers;

import com.knuddels.jtokkit.api.EncodingType;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/** Inspired on silly tavern's tokenizer.js */
@Component
final class TokenizerRegistry {
    private static final String RESOURCE_ROOT = "tokenizers/";
    private static final String TOKENIZER_BUNDLE_REVISION =
            "tokenizers-v1";

    private final Map<String, Supplier<Tokenizer>> factories;
    private final ConcurrentMap<String, Tokenizer> loadedTokenizers =
            new ConcurrentHashMap<>();

    TokenizerRegistry() {
        this.factories = Map.ofEntries(
                // SentencePiece models
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

                // Hugging Face tokenizer JSON files
                Map.entry(
                        "claude",
                        () -> huggingFaceJson("claude.json")
                ),
                Map.entry(
                        "llama3",
                        () -> huggingFaceJson("llama3.json")
                ),
                Map.entry(
                        "deepseekv4",
                        () -> huggingFaceJson("deepseekv4.json")
                ),
                Map.entry(
                        "glm",
                        () -> huggingFaceJson("glm.json")
                ),

                // Built-in JTokkit encodings
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

    public Tokenizer getForModel(String modelId) {
        String tokenizerId = resolveTokenizerId(modelId);

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
         * Specific model families must be checked before generic ones.
         */

        if (containsAny(
                model,
                "deepseek-v4",
                "deepseek_v4",
                "deepseekv4",
                "deepseek/v4"
        )) {
            return "deepseekv4";
        }

        if (containsAny(
                model,
                "glm-5",
                "glm5",
                "glm_5",
                "zai-org/glm",
                "zhipu/glm"
        )) {
            return "glm";
        }

        if (containsAny(
                model,
                "nerdstash_v2",
                "nerdstash-v2"
        )) {
            return "nerdstash_v2";
        }

        if (model.contains("nerdstash")) {
            return "nerdstash";
        }

        if (containsAny(
                model,
                "llama3",
                "llama-3"
        )) {
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

        if (containsAny(
                model,
                "gemma",
                "gemini",
                "learnlm"
        )) {
            return "gemma";
        }

        if (model.contains("claude")) {
            return "claude";
        }

        if (model.contains("yi")) {
            return "yi";
        }

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

        /*
         * Distilled R1 models use their underlying Qwen or Llama tokenizer.
         * Check these before the generic "deepseek" branch.
         */
        if (containsAny(
                model,
                "deepseek-r1-distill-qwen",
                "r1-distill-qwen"
        )) {
            throw new IllegalArgumentException(
                    "DeepSeek R1 Qwen distill requires a Qwen tokenizer: "
                            + modelId
            );
        }

        if (containsAny(
                model,
                "deepseek-r1-distill-llama",
                "r1-distill-llama"
        )) {
            return "llama3";
        }

        /*
         * DeepSeek Coder has its own ByteLevel-BPE tokenizer and
         * should not be mapped to the mainline DeepSeek tokenizer.
         */
        if (containsAny(
                model,
                "deepseek-coder",
                "deepseekcoder"
        )) {
            throw new IllegalArgumentException(
                    "DeepSeek Coder requires its own tokenizer: "
                            + modelId
            );
        }

        /*
         * Mainline DeepSeek fallback:
         *
         * DeepSeek V2/V2.5/V3/V3.1/V3.2/V4
         * DeepSeek Chat
         * DeepSeek Reasoner
         * Full-size DeepSeek R1/R1-Zero
         */
        if (model.contains("deepseek")) {
            return "deepseekv4";
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
    public record ResolvedTokenizer(
            String id,
            String revision,
            Tokenizer tokenizer
    ) {}

    public ResolvedTokenizer resolveForModel(String modelId) {
        String tokenizerId = resolveTokenizerId(modelId);

        Tokenizer tokenizer = loadedTokenizers.computeIfAbsent(
                tokenizerId,
                ignored -> createTokenizer(tokenizerId)
        );

        return new ResolvedTokenizer(
                tokenizerId,
                TOKENIZER_BUNDLE_REVISION,
                tokenizer
        );
    }

    @PreDestroy
    public void closeLoadedTokenizers() {
        for (Tokenizer tokenizer : loadedTokenizers.values()) {
            if (tokenizer instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                    // Application is shutting down.
                }
            }
        }

        loadedTokenizers.clear();
    }
}