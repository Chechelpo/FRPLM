package io.github.chechelpo.frplm.core.prompt.building;


import io.github.chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import io.github.chechelpo.frplm.domain.prompts.section.DefaultSections;
import io.github.chechelpo.frplm.extensions.api.prompts.PromptSection;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSectionEntitySnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.jetbrains.annotations.Contract;
import org.jooq.Record2;
import org.jooq.Result;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SectionManager {
    private final String unrenderedContent;

    private final ChatCompletionRole role;
    private String renderedContent;
    private final PromptSection.InjectAtPosition position;
    private final boolean isChatHistorySection;

    public SectionManager(PromptSectionEntitySnapshot section){
        Objects.requireNonNull(section, "Section is null");
        this.unrenderedContent = section.content();
        this.role = section.role();
        this.position = section.getInjectionOrder();
        this.isChatHistorySection = section.asReference().promptId() == DefaultSections.CHAT_HISTORY.sectionID;
    }
    public SectionManager(String content, ChatCompletionRole role, PromptSection.InjectAtPosition position){
        this.unrenderedContent = content;
        this.role = role;
        this.position = position;
        this.isChatHistorySection = false;
    }

    @Contract(" -> new")
    @NonNull ChatCompletionMessage asCompletionMessage(){
        if (renderedContent == null)
            throw new IllegalStateException("Called for completion message with no rendered content");
        return new ChatCompletionMessage(role, null, StandardOutlet.stripUnresolvedMacros(renderedContent));
    }

    PromptSection.InjectAtPosition getInjectionOrder(){
        return position;
    }

    boolean isChatHistorySection(){
        return isChatHistorySection;
    }

    void injectEntriesAtDetectedOutlets(LorebooksManager lorebookManager, PromptRenderer promptRenderer){
        String result = unrenderedContent;

        Result<Record2<Integer, String>> lorebookOutlets = lorebookManager.getOutlets();
        for (Record2<Integer, String> outlet : lorebookOutlets){
            int outletId = outlet.component1();
            Pattern pattern = StandardOutlet.asPattern(outlet.component2());
            Optional<String> toInject = promptRenderer.renderEligibleAtOutlet(outletId, lorebookManager);
            if (toInject.isPresent() && !toInject.get().isBlank())
                result = pattern
                        .matcher(result)
                        .replaceAll(Matcher.quoteReplacement(toInject.get()));


        }

        renderedContent = result;
    }


    private void renderRelevantEntries(LorebooksManager manager){

    }

    public String getUnrenderedContent(){
        return unrenderedContent;
    }
}
