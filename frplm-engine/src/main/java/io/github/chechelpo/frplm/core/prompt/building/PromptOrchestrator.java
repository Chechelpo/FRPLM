package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.core.prompt.TextType;
import io.github.chechelpo.frplm.domain.lorebook.LorebookContext;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.NotInitialized;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.extensions.api.prompts.PromptBuilder;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import io.github.chechelpo.frplm.extensions.api.session.Session;
import io.github.chechelpo.frplm.extensions.api.session.SessionPrompt;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSectionEntitySnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PromptOrchestrator implements PromptBuilder {

    private final PromptBudgetManager tokensManager;
    private final PromptRenderer promptBuilder;
    private final LorebooksManager lorebookManager;
    private final Session session;

    public PromptOrchestrator(
            @NonNull PromptBudgetManager tokensManager,
            LorebookContext lorebookContext,
            Session session
    ) {
        List<ChatMessage> messages = tokensManager.fillChatHistoryBudget(session);

        List<PromptSectionEntitySnapshot> sections = session.getPrompt()
                .orElseThrow(() -> new NotInitialized("Prompt is not initialized", Severity.EXPECTED))
                .getSections().stream()
                .peek(section -> {
                    if (!tokensManager.hasSpaceFor(section.content(), TextType.PROMPT_SECTION))
                        throw new UnexpectedException("Token use of prompt section is larger than the available budget", Severity.USER);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        this.session = session;
        this.promptBuilder = new PromptRenderer(messages, sections);
        this.tokensManager = tokensManager;
        this.lorebookManager = new LorebooksManager(lorebookContext, tokensManager);
    }


    public PromptResult render(){
        lorebookManager.activateEntries(promptBuilder);
        List<ChatCompletionMessage> messages = promptBuilder.render(lorebookManager);
        SessionPrompt prompt = session.getPrompt().orElseThrow();

        return new PromptResult(
                ChatCompletionRequest.builder()
                    .appendAll(messages)
                    .configurationParameters(prompt.getGenerationConfig())
                    .generationParameters(prompt.getParameters())
                    .modelID(prompt.getAssignedConnection().orElseThrow().getModelID())
                    .build(),
                lorebookManager
        );
    }

    @Override
    public @UnmodifiableView List<LorebookSnapshot> getLorebooks() {
        return lorebookManager.getLorebooks();
    }

    @Override
    public PromptBuilder addLorebook(LorebookSnapshot lorebook) {
        lorebookManager.addLorebook(lorebook);
        return this;
    }

    @Override
    public PromptBuilder addLorebooks(List<LorebookSnapshot> lorebooks) {
        lorebookManager.addLorebooks(lorebooks);
        return this;
    }

    @Override
    public PromptBuilder addLorebooks(LorebookSnapshot... lorebookSnapshots) {
        return null;
    }

    @Override
    public PromptBuilder appendAsSection(ChatCompletionMessage section) {
        return null;
    }

    @Override
    public PromptBuilder append(@NotNull ChatMessage message) {
        return null;
    }

    @Override
    public PromptBuilder appendAll(@NotNull List<ChatMessage> chatHistory) {
        return null;
    }

    @Override
    public PromptBuilder insertAt(int depth, ChatCompletionMessage message) {
        return null;
    }

    @Override
    public PromptBuilder injectAtOutlet(int id, String content) {
        return null;
    }
}
