package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.core.prompt.TextType;
import io.github.chechelpo.frplm.domain.lorebook.LorebookContext;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.NotInitialized;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.extensions.api.prompts.OutletManager;
import io.github.chechelpo.frplm.extensions.api.prompts.PromptBuilder;
import io.github.chechelpo.frplm.extensions.api.prompts.PromptSection;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import io.github.chechelpo.frplm.extensions.api.session.Session;
import io.github.chechelpo.frplm.extensions.api.session.SessionPrompt;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSectionEntitySnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PromptOrchestrator implements PromptBuilder {

    private final PromptBudgetManager tokensManager;
    private final PromptRenderer promptBuilder;
    private final MacroManager macroManager;

    private final LorebookManagerImpl lorebookManager;
    private final OutletManagerImpl outletManager;

    private final Session session;

    public PromptOrchestrator(
            @NonNull PromptBudgetManager tokensManager,
            LorebookContext lorebookContext,
            Session session
    ) {
        List<ChatMessage> messages = tokensManager.fillChatHistoryBudget(session);

        List<PromptSectionEntitySnapshot> sections = getSections(tokensManager, session);
        this.session = session;
        this.promptBuilder = new PromptRenderer(messages, sections.stream().map(SectionManager::new).toList());
        this.tokensManager = tokensManager;

        this.lorebookManager = new LorebookManagerImpl(lorebookContext.entries, lorebookContext.entryKeywords, tokensManager);
        this.outletManager = new OutletManagerImpl(lorebookContext.outlets, lorebookManager);
        this.macroManager = new MacroManager(outletManager);
    }

    public PromptOrchestrator(
            @NonNull PromptBudgetManager tokensManager,
            LorebookContext lorebookContext
    ) {
        session = null;

        this.promptBuilder = new PromptRenderer(List.of());
        this.tokensManager = tokensManager;

        this.lorebookManager = new LorebookManagerImpl(lorebookContext.entries, lorebookContext.entryKeywords, tokensManager);
        this.outletManager = new OutletManagerImpl(lorebookContext.outlets, lorebookManager);
        this.macroManager = new MacroManager(outletManager);
    }

    private static @NonNull ArrayList<PromptSectionEntitySnapshot> getSections(@NonNull PromptBudgetManager tokensManager, Session session) {
        return session.getPrompt()
                .orElseThrow(notFound ->
                        new NotInitialized("Prompt is not initialized: \n" + notFound.toDebugString(), Severity.USER)
                ).getSections().stream()
                .peek(section -> {
                    if (!tokensManager.hasSpaceFor(section.content(), TextType.PROMPT_SECTION))
                        throw new UnexpectedException("Token use of prompt section is larger than the available budget", Severity.USER);
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Optional<Session> getSession(){
        return Optional.ofNullable(session);
    }

    public PromptResult render(){
        lorebookManager.activateEntries(promptBuilder);
        macroManager.addEntries(lorebookManager.activeEntries());

        List<ChatCompletionMessage> messages = promptBuilder.render(macroManager);
        SessionPrompt prompt = session.getPrompt().orElseThrow();

        return new PromptResult(
                ChatCompletionRequest.builder()
                    .appendAll(messages)
                    .configurationParameters(prompt.getGenerationConfig())
                    .generationParameters(prompt.getParameters())
                    .modelID(prompt.getAssignedConnection()
                            .orElseThrow().getModelID())
                    .build(),
                lorebookManager
        );
    }

    @Override
    public @UnmodifiableView List<LorebookSnapshot> getLorebooks() {
        return lorebookManager.activeLorebooks();
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
        Arrays.stream(lorebookSnapshots).forEach(this::addLorebook);
        return null;
    }

    @Override
    public PromptBuilder appendAsSection(ChatCompletionMessage section) {
        promptBuilder.addSection(section, new PromptSection.InjectAtPosition.AtDepth(0));
        return this;
    }

    @Override
    public PromptBuilder insertAt(int depth, ChatCompletionMessage message) {
        System.out.println("Inserted " + message + " at position " + depth);
        promptBuilder.addSection(message, new PromptSection.InjectAtPosition.AtDepth(depth));
        return this;
    }

    @Override
    public PromptBuilder appendAtMacro(String macro, String content) {
        macroManager.appendAtMacro(macro, content);
        return this;
    }

    @Override
    public PromptBuilder prependAtMacro(String macro, String content) {
        macroManager.prependAtMacro(macro, content);
        return this;
    }

    @Override
    public PromptBuilder addAtMacro(String macro, String content, int atIndex) {
        macroManager.injectAtMacro(macro, content, atIndex);
        return this;
    }

    @Override
    public OutletManager getOutletManager() {
        return outletManager;
    }
}
