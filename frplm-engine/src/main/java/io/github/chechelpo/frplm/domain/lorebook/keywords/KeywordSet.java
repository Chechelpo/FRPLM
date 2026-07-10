package io.github.chechelpo.frplm.domain.lorebook.keywords;

import it.unimi.dsi.fastutil.ints.IntSet;

public record KeywordSet(IntSet fromPrompt, IntSet fromChatHistory) {}
