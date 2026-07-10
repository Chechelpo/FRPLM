package io.github.chechelpo.frplm.utils.tokenizers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.OptionalInt;

@Component
public final class TokenizerService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TokenizerService.class);

    private final TokenizerRegistry registry;
    private final TokenizerStore store;

    TokenizerService(
            TokenizerRegistry registry,
            TokenizerStore store
    ) {
        this.registry = registry;
        this.store = store;
    }

    /**
     * Counts a raw string using the tokenizer selected for the model.
     */
    public int tokenCount(String modelId, String text) {
        return tokenCount(
                modelId,
                text,
                TokenizationMode.RAW_TEXT
        );
    }

    /**
     * Counts an already-rendered string and namespaces its cache entry
     * by tokenization mode.
     * <p>
     * The mode does not transform the text. A CHAT_PROMPT value should
     * therefore already contain the model-specific chat serialization.
     */
    public int tokenCount(
            String modelId,
            String text,
            TokenizationMode mode
    ) {
        validateArguments(modelId, text, mode);

        TokenizerRegistry.ResolvedTokenizer resolved =
                registry.resolveForModel(modelId);

        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        String contentHash = sha256(utf8);

        TokenizerStore.CacheKey cacheKey =
                new TokenizerStore.CacheKey(
                        resolved.id(),
                        resolved.revision(),
                        mode,
                        contentHash
                );

        OptionalInt cached = findCached(cacheKey);

        if (cached.isPresent()) {
            return cached.getAsInt();
        }

        int tokenCount = resolved.tokenizer().tokenCount(text);

        if (tokenCount < 0) {
            throw new IllegalStateException(
                    "Tokenizer returned a negative token count: "
                            + resolved.id()
            );
        }

        cacheComputedValue(
                cacheKey,
                utf8.length,
                tokenCount
        );

        return tokenCount;
    }

    private OptionalInt findCached(
            TokenizerStore.CacheKey cacheKey
    ) {
        try {
            return store.findAndTouch(cacheKey);
        } catch (DataAccessException exception) {
            /*
             * Tokenization must remain operational if the cache table
             * is unavailable. The cache is reconstructible data.
             */
            LOGGER.warn(
                    "Could not read tokenizer cache for tokenizer {}",
                    cacheKey.tokenizerId(),
                    exception
            );

            return OptionalInt.empty();
        }
    }

    private void cacheComputedValue(
            TokenizerStore.CacheKey cacheKey,
            int byteLength,
            int tokenCount
    ) {
        try {
            store.putIfAbsent(
                    cacheKey,
                    byteLength,
                    tokenCount
            );
        } catch (DataAccessException exception) {
            LOGGER.warn(
                    "Could not persist tokenizer cache entry for tokenizer {}",
                    cacheKey.tokenizerId(),
                    exception
            );
        }
    }

    private static void validateArguments(
            String modelId,
            String text,
            TokenizationMode mode
    ) {
        Objects.requireNonNull(
                modelId,
                "modelId must not be null"
        );
        Objects.requireNonNull(
                text,
                "text must not be null"
        );
        Objects.requireNonNull(
                mode,
                "mode must not be null"
        );

        if (modelId.isBlank()) {
            throw new IllegalArgumentException(
                    "modelId must not be blank"
            );
        }
    }

    /**
     * Hashes the exact UTF-8 representation. No trimming or Unicode
     * normalization is performed because that could alter tokenization.
     */
    private static String sha256(byte[] content) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(content);

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "The JVM does not provide SHA-256",
                    exception
            );
        }
    }
}
