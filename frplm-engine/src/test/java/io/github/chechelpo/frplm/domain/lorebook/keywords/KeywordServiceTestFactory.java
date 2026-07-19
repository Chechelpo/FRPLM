package io.github.chechelpo.frplm.domain.lorebook.keywords;

import static org.mockito.Mockito.mock;

public final class KeywordServiceTestFactory {

    private KeywordServiceTestFactory() {}

    public static KeywordService mockService() {
        return mock(KeywordServiceImpl.class);
    }
}