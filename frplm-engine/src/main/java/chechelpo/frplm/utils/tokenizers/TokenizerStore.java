package chechelpo.frplm.utils.tokenizers;

import chechelpo.frplm.annotations.Store;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.OptionalInt;

import static chechelpo.frplm.jooq.generated.Tables.TOKENIZER_CACHE;

@Store
class TokenizerStore {

    private final DSLContext dsl;

    TokenizerStore(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Finds an existing token count and updates its usage metadata.
     */
    @Transactional
    public OptionalInt findAndTouch(CacheKey key) {
        Objects.requireNonNull(key, "key must not be null");

        Condition condition = matches(key);

        Integer tokenCount = dsl
                .select(TOKENIZER_CACHE.TOKEN_COUNT)
                .from(TOKENIZER_CACHE)
                .where(condition)
                .fetchOne(TOKENIZER_CACHE.TOKEN_COUNT);

        if (tokenCount == null) {
            return OptionalInt.empty();
        }

        dsl.update(TOKENIZER_CACHE)
                .set(
                        TOKENIZER_CACHE.LAST_USED,
                        DSL.currentLocalDateTime()
                )
                .set(
                        TOKENIZER_CACHE.HIT_COUNT,
                        TOKENIZER_CACHE.HIT_COUNT.plus(1L)
                )
                .where(condition)
                .execute();

        return OptionalInt.of(tokenCount);
    }

    /**
     * Inserts a cache entry unless another request has already inserted
     * the same primary key.
     */
    @Transactional
    public void putIfAbsent(
            CacheKey key,
            int contentByteLength,
            int tokenCount
    ) {
        Objects.requireNonNull(key, "key must not be null");

        if (contentByteLength < 0) {
            throw new IllegalArgumentException(
                    "contentByteLength must not be negative"
            );
        }

        if (tokenCount < 0) {
            throw new IllegalArgumentException(
                    "tokenCount must not be negative"
            );
        }

        dsl.insertInto(TOKENIZER_CACHE)
                .set(
                        TOKENIZER_CACHE.TOKENIZER_ID,
                        key.tokenizerId()
                )
                .set(
                        TOKENIZER_CACHE.TOKENIZER_REVISION,
                        key.tokenizerRevision()
                )
                .set(
                        TOKENIZER_CACHE.TOKENIZATION_MODE,
                        key.mode().name()
                )
                .set(
                        TOKENIZER_CACHE.CONTENT_HASH,
                        key.contentHash()
                )
                .set(
                        TOKENIZER_CACHE.CONTENT_BYTE_LENGTH,
                        contentByteLength
                )
                .set(
                        TOKENIZER_CACHE.TOKEN_COUNT,
                        tokenCount
                )
                /*
                 * jOOQ translates this to the appropriate dialect-specific
                 * operation, such as ON CONFLICT DO NOTHING.
                 */
                .onDuplicateKeyIgnore()
                .execute();
    }

    /**
     * Removes reconstructible cache entries that have not been used
     * since the specified time.
     *
     * @return number of deleted rows
     */
    @Transactional
    public int deleteUnusedBefore(LocalDateTime cutoff) {
        Objects.requireNonNull(cutoff, "cutoff must not be null");

        return dsl.deleteFrom(TOKENIZER_CACHE)
                .where(TOKENIZER_CACHE.LAST_USED.lt(cutoff))
                .execute();
    }

    private static Condition matches(CacheKey key) {
        return TOKENIZER_CACHE.TOKENIZER_ID
                .eq(key.tokenizerId())
                .and(
                        TOKENIZER_CACHE.TOKENIZER_REVISION
                                .eq(key.tokenizerRevision())
                )
                .and(
                        TOKENIZER_CACHE.TOKENIZATION_MODE
                                .eq(key.mode().name())
                )
                .and(
                        TOKENIZER_CACHE.CONTENT_HASH
                                .eq(key.contentHash())
                );
    }

    public record CacheKey(
            String tokenizerId,
            String tokenizerRevision,
            TokenizationMode mode,
            String contentHash
    ) {
        public CacheKey {
            Objects.requireNonNull(
                    tokenizerId,
                    "tokenizerId must not be null"
            );
            Objects.requireNonNull(
                    tokenizerRevision,
                    "tokenizerRevision must not be null"
            );
            Objects.requireNonNull(
                    mode,
                    "mode must not be null"
            );
            Objects.requireNonNull(
                    contentHash,
                    "contentHash must not be null"
            );

            if (tokenizerId.isBlank()) {
                throw new IllegalArgumentException(
                        "tokenizerId must not be blank"
                );
            }

            if (tokenizerRevision.isBlank()) {
                throw new IllegalArgumentException(
                        "tokenizerRevision must not be blank"
                );
            }

            if (contentHash.length() != 64) {
                throw new IllegalArgumentException(
                        "contentHash must be a hexadecimal SHA-256 hash"
                );
            }
        }
    }
}
