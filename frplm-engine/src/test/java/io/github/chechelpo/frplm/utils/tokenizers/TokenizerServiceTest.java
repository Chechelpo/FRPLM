package io.github.chechelpo.frplm.utils.tokenizers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenizerServiceTest {

    private static final String MODEL_ID = "deepseek-v4";
    private static final String TOKENIZER_ID = "deepseekv4";
    private static final String TOKENIZER_REVISION = "tokenizers-v1";

    @Mock
    private TokenizerRegistry registry;

    @Mock
    private TokenizerStore store;

    @Mock
    private Tokenizer tokenizer;

    private TokenizerService service;

    @BeforeEach
    void setUp() {
        service = new TokenizerService(registry, store);
    }

    @Test
    void shouldReturnCachedTokenCountWithoutRunningTokenizer() {
        String text = "Hello tokenizer";

        TokenizerRegistry.ResolvedTokenizer resolved =
                resolvedTokenizer();

        when(registry.resolveForModel(MODEL_ID))
                .thenReturn(resolved);

        when(store.findAndTouch(any(TokenizerStore.CacheKey.class)))
                .thenReturn(OptionalInt.of(17));

        int result = service.tokenCount(MODEL_ID, text);

        assertEquals(17, result);

        verify(registry).resolveForModel(MODEL_ID);
        verify(store).findAndTouch(any(TokenizerStore.CacheKey.class));

        verifyNoInteractions(tokenizer);

        verify(
                store,
                never()
        ).putIfAbsent(
                any(TokenizerStore.CacheKey.class),
                anyInt(),
                anyInt()
        );
    }

    @Test
    void shouldTokenizeAndCacheOnCacheMiss() {
        String text = "Hello, 世界";
        int expectedTokenCount = 9;

        when(registry.resolveForModel(MODEL_ID))
                .thenReturn(resolvedTokenizer());

        when(store.findAndTouch(any(TokenizerStore.CacheKey.class)))
                .thenReturn(OptionalInt.empty());

        when(tokenizer.tokenCount(text))
                .thenReturn(expectedTokenCount);

        int result = service.tokenCount(MODEL_ID, text);

        assertEquals(expectedTokenCount, result);

        ArgumentCaptor<TokenizerStore.CacheKey> keyCaptor =
                ArgumentCaptor.forClass(
                        TokenizerStore.CacheKey.class
                );

        verify(store).putIfAbsent(
                keyCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(
                        text.getBytes(StandardCharsets.UTF_8).length
                ),
                org.mockito.ArgumentMatchers.eq(expectedTokenCount)
        );

        TokenizerStore.CacheKey cacheKey =
                keyCaptor.getValue();

        assertEquals(
                TOKENIZER_ID,
                cacheKey.tokenizerId()
        );

        assertEquals(
                TOKENIZER_REVISION,
                cacheKey.tokenizerRevision()
        );

        assertEquals(
                TokenizationMode.RAW_TEXT,
                cacheKey.mode()
        );

        assertEquals(
                64,
                cacheKey.contentHash().length(),
                "SHA-256 should produce 64 hexadecimal characters"
        );

        verify(tokenizer).tokenCount(text);
    }

    @Test
    void shouldUseExpectedSha256Hash() {
        String text = "Hello";

        when(registry.resolveForModel(MODEL_ID))
                .thenReturn(resolvedTokenizer());

        when(store.findAndTouch(any(TokenizerStore.CacheKey.class)))
                .thenReturn(OptionalInt.empty());

        when(tokenizer.tokenCount(text))
                .thenReturn(1);

        service.tokenCount(MODEL_ID, text);

        ArgumentCaptor<TokenizerStore.CacheKey> keyCaptor =
                ArgumentCaptor.forClass(
                        TokenizerStore.CacheKey.class
                );

        verify(store).putIfAbsent(
                keyCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq(1)
        );

        assertEquals(
                "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969",
                keyCaptor.getValue().contentHash()
        );
    }

    @Test
    void shouldNamespaceCacheByTokenizationMode() {
        String text = "<|user|>Hello";

        when(registry.resolveForModel(MODEL_ID))
                .thenReturn(resolvedTokenizer());

        when(store.findAndTouch(any(TokenizerStore.CacheKey.class)))
                .thenReturn(OptionalInt.empty());

        when(tokenizer.tokenCount(text))
                .thenReturn(4);

        service.tokenCount(
                MODEL_ID,
                text,
                TokenizationMode.CHAT_PROMPT
        );

        ArgumentCaptor<TokenizerStore.CacheKey> keyCaptor =
                ArgumentCaptor.forClass(
                        TokenizerStore.CacheKey.class
                );

        verify(store).putIfAbsent(
                keyCaptor.capture(),
                anyInt(),
                org.mockito.ArgumentMatchers.eq(4)
        );

        assertEquals(
                TokenizationMode.CHAT_PROMPT,
                keyCaptor.getValue().mode()
        );
    }

    @Test
    void shouldStillTokenizeWhenCacheReadFails() {
        String text = "Cache unavailable";

        when(registry.resolveForModel(MODEL_ID))
                .thenReturn(resolvedTokenizer());

        when(store.findAndTouch(any(TokenizerStore.CacheKey.class)))
                .thenThrow(
                        new DataAccessResourceFailureException(
                                "Database unavailable"
                        )
                );

        when(tokenizer.tokenCount(text))
                .thenReturn(5);

        int result = service.tokenCount(MODEL_ID, text);

        assertEquals(5, result);

        verify(tokenizer).tokenCount(text);

        verify(store).putIfAbsent(
                any(TokenizerStore.CacheKey.class),
                org.mockito.ArgumentMatchers.eq(
                        text.getBytes(StandardCharsets.UTF_8).length
                ),
                org.mockito.ArgumentMatchers.eq(5)
        );
    }

    @Test
    void shouldStillReturnCountWhenCacheWriteFails() {
        String text = "Write failure";

        when(registry.resolveForModel(MODEL_ID))
                .thenReturn(resolvedTokenizer());

        when(store.findAndTouch(any(TokenizerStore.CacheKey.class)))
                .thenReturn(OptionalInt.empty());

        when(tokenizer.tokenCount(text))
                .thenReturn(3);

        doThrow(
                new DataAccessResourceFailureException(
                        "Database unavailable"
                )
        ).when(store).putIfAbsent(
                any(TokenizerStore.CacheKey.class),
                anyInt(),
                anyInt()
        );

        int result = service.tokenCount(MODEL_ID, text);

        assertEquals(3, result);
        verify(tokenizer).tokenCount(text);
    }

    @Test
    void shouldRejectNegativeTokenizerResult() {
        String text = "Invalid tokenizer";

        when(registry.resolveForModel(MODEL_ID))
                .thenReturn(resolvedTokenizer());

        when(store.findAndTouch(any(TokenizerStore.CacheKey.class)))
                .thenReturn(OptionalInt.empty());

        when(tokenizer.tokenCount(text))
                .thenReturn(-1);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.tokenCount(MODEL_ID, text)
                );

        assertEquals(
                "Tokenizer returned a negative token count: deepseekv4",
                exception.getMessage()
        );

        verify(
                store,
                never()
        ).putIfAbsent(
                any(TokenizerStore.CacheKey.class),
                anyInt(),
                anyInt()
        );
    }

    @Test
    void shouldRejectNullModelId() {
        assertThrows(
                NullPointerException.class,
                () -> service.tokenCount(null, "text")
        );

        verifyNoInteractions(registry, store, tokenizer);
    }

    @Test
    void shouldRejectBlankModelId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.tokenCount("   ", "text")
        );

        verifyNoInteractions(registry, store, tokenizer);
    }

    @Test
    void shouldRejectNullText() {
        assertThrows(
                NullPointerException.class,
                () -> service.tokenCount(MODEL_ID, null)
        );

        verifyNoInteractions(registry, store, tokenizer);
    }

    @Test
    void shouldRejectNullMode() {
        assertThrows(
                NullPointerException.class,
                () -> service.tokenCount(
                        MODEL_ID,
                        "text",
                        null
                )
        );

        verifyNoInteractions(registry, store, tokenizer);
    }

    @Test
    void shouldAllowEmptyText() {
        String text = "";

        when(registry.resolveForModel(MODEL_ID))
                .thenReturn(resolvedTokenizer());

        when(store.findAndTouch(any(TokenizerStore.CacheKey.class)))
                .thenReturn(OptionalInt.empty());

        when(tokenizer.tokenCount(text))
                .thenReturn(0);

        int result = service.tokenCount(MODEL_ID, text);

        assertEquals(0, result);

        verify(store).putIfAbsent(
                any(TokenizerStore.CacheKey.class),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(0)
        );
    }

    private TokenizerRegistry.ResolvedTokenizer resolvedTokenizer() {
        return new TokenizerRegistry.ResolvedTokenizer(
                TOKENIZER_ID,
                TOKENIZER_REVISION,
                tokenizer
        );
    }
}
