package io.github.chechelpo.frplm.domain.lorebook.keywords;

public sealed interface KeywordService permits KeywordServiceImpl {
    int getOrGenerate(String keyword);

}
