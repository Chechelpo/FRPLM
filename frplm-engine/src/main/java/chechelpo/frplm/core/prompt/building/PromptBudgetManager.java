package chechelpo.frplm.core.prompt.building;

import chechelpo.frplm.core.prompt.TextType;
import chechelpo.frplm.utils.tokenizers.TokenizationMode;
import chechelpo.frplm.utils.tokenizers.TokenizerService;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import io.github.chechelpo.frplm.extensions.api.session.Session;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSnapshot;

import java.util.ArrayList;
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

    private int lorebookConsumedTokens = 0;
    private int generalConsumedBudget = 0;
    private int consumedChatHistoryBudget = 0;

    public PromptBudgetManager(String modelId, PromptSnapshot promptTemplate, TokenizerService tokenizer) {
        Objects.requireNonNull(promptTemplate, "Prompt template is null");
        Objects.requireNonNull(tokenizer, "Tokenizer is null");
        Objects.requireNonNull(modelId, "Model id is null");

        this.modelId = modelId;
        int maxTokens = promptTemplate.getGenerationConfig().max_tokens();

        this.lorebookBudget = (int) Math.floor(maxTokens * promptTemplate.getBudgetConfig().lorebookBudget());
        this.chatHistoryBudget = (int) Math.floor(maxTokens * promptTemplate.getBudgetConfig().chatBudget());
        this.generalBudget = maxTokens - lorebookBudget - consumedChatHistoryBudget;

        this.tokenizer = tokenizer;
    }

    public int getConsumedTokens(){
        return lorebookConsumedTokens + generalConsumedBudget;
    }

    /** @return a list of messages that have been budgeted for */
    public List<ChatMessage> fillChatHistoryBudget(Session session) {
        Objects.requireNonNull(session, "Message service is null");

        final int step = 10;
        List<ChatMessage> result = new ArrayList<>(step * 3);

        int from = 0;
        int to = step - 1;

        while (consumedChatHistoryBudget < chatHistoryBudget) {
            List<ChatMessage> batch = session.getMessageRange(from, to);

            if (batch.isEmpty()) {
                break;
            }

            for (ChatMessage message : batch) {
                String content = message.content();
                if (content == null || content.isBlank()) {
                    continue;
                }

                int tokenCount = tokenizer.tokenCount(modelId, content, TokenizationMode.RAW_TEXT);

                if (consumedChatHistoryBudget + tokenCount > chatHistoryBudget) {
                    generalBudget += chatHistoryBudget - consumedChatHistoryBudget;
                    return result;
                }

                consumedChatHistoryBudget += tokenCount;
                result.add(message);
            }

            from += step;
            to += step;
        }

        // Whatever's left is dumped into the general tokens.
        generalBudget += chatHistoryBudget - consumedChatHistoryBudget;

        return result;
    }

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
