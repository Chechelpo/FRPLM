package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.core.prompt.TextType;
import io.github.chechelpo.frplm.extensions.api.utils.PromptBudget;
import io.github.chechelpo.frplm.utils.tokenizers.TokenizationMode;
import io.github.chechelpo.frplm.utils.tokenizers.TokenizerService;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import io.github.chechelpo.frplm.extensions.api.session.Session;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * In charge of keeping track of the token budget of a given prompt.
 * <ul>
 *     <li> {@link TextType#WORLD_CONTEXT} sections are ALWAYS given ok, they are considered the most important </li>
 *     <li> {@link TextType#LOREBOOK_ENTRY} are given ok if the lorebook budget has not been filled yet </li>
 *     <li> {@link TextType#CHAT_HISTORY}, {@link TextType#PROMPT_SECTION} sections are given ok if there is enough free tokens </li>
 * </ul>
 */
public class PromptBudgetManager {
    private final String modelId;
    private final TokenizerService tokenizer;

    private final int lorebookBudget;
    private int generalBudget;
    private final int chatHistoryBudget;
    private boolean chatHistoryBudgetFilled;

    private int lorebookConsumedTokens = 0;
    private int generalConsumedBudget = 0;
    private int consumedChatHistoryBudget = 0;

    public PromptBudgetManager(String modelId, PromptBudget budget, TokenizerService tokenizer) {
        Objects.requireNonNull(budget, "Prompt template is null");
        Objects.requireNonNull(tokenizer, "Tokenizer is null");
        Objects.requireNonNull(modelId, "Model id is null");

        this.modelId = modelId;
        int maxTokens = budget.maxTokens();

        this.lorebookBudget = (int) Math.floor(maxTokens * budget.lorebookRatio());
        this.chatHistoryBudget = (int) Math.floor(maxTokens * budget.chatRatio());
        this.generalBudget = maxTokens - lorebookBudget - chatHistoryBudget;

        this.tokenizer = tokenizer;
    }

    public int getConsumedTokens() {
        return lorebookConsumedTokens
                + generalConsumedBudget
                + consumedChatHistoryBudget;
    }

    /**
     * Fetches all possible messages of the session that are allowed, using {@link Session#getLastMessagesRange(int, int)}
     * @return a list of messages that have been budgeted for
     */
    public List<ChatMessage> fillChatHistoryBudget(Session session) {
        if (chatHistoryBudgetFilled) throw new IllegalStateException("Fill chat history budget called twice");
        chatHistoryBudgetFilled = true;
        Objects.requireNonNull(session, "Session is null");
        final int step = 10;

        int consumed = 0;
        List<ChatMessage> result = new ArrayList<>(step * 3);

        int to = session.getCurrentTick();

        while (to >= 0 && consumed < chatHistoryBudget) {
            int from = Math.max(0, to - step + 1);

            // Expected order: newest to oldest within the range.
            List<ChatMessage> batch = session.getLastMessagesRange(from, to);

            for (ChatMessage message : batch) {
                String content = message.content();

                if (content == null || content.isBlank()) {
                    continue;
                }

                int tokenCount = tokenizer.tokenCount(
                        modelId,
                        content,
                        TokenizationMode.RAW_TEXT
                );

                if (consumed + tokenCount > chatHistoryBudget) {
                    generalBudget += chatHistoryBudget - consumed;

                    // Messages were collected newest-first.
                    Collections.reverse(result);
                    consumedChatHistoryBudget = consumed;
                    return result;
                }

                consumed += tokenCount;
                result.add(message);
            }

            if (from == 0) {
                break;
            }

            to = from - 1;
        }

        generalBudget += chatHistoryBudget - consumed;
        consumedChatHistoryBudget = consumed;

        // Convert newest-first selection into chronological chat order.
        Collections.reverse(result);

        return result;
    }
    /** Budgets for this text */
    @Contract(mutates = "this")
    public boolean hasSpaceFor(String text, TextType sectionType) {
        Objects.requireNonNull(text, "Text to budget is null");
        Objects.requireNonNull(sectionType, "Section type is null");

        final int tokenCount = tokenizer.tokenCount(modelId, text, TokenizationMode.RAW_TEXT);

        return switch (sectionType){
            case WORLD_CONTEXT -> {
                generalConsumedBudget += tokenCount;
                yield true;
            }
            case CHAT_HISTORY, PROMPT_SECTION -> budgetForGeneral(tokenCount);
            case LOREBOOK_ENTRY -> budgetLorebook(tokenCount);
        };
    }

    private boolean budgetForGeneral(int tokenCount){
        if (tokenCount + generalConsumedBudget > generalBudget) return false;
        generalConsumedBudget += tokenCount;
        return true;
    }

    private boolean budgetLorebook(int tokenCount){
        if (tokenCount + lorebookConsumedTokens > lorebookBudget) return false;
        lorebookConsumedTokens += tokenCount;
        return true;
    }
}
