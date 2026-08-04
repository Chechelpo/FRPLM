package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.extensions.api.prompts.PromptSection;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSectionEntitySnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import io.github.chechelpo.frplm.utils.macros.Macro;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.Optional;

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
        this.isChatHistorySection = section.type() == PromptSectionEntitySnapshot.Type.CHAT_HISTORY;
    }

    public SectionManager(String content, ChatCompletionRole role, PromptSection.InjectAtPosition position){
        this.unrenderedContent = content;
        this.role = role;
        this.position = position;
        this.isChatHistorySection = false;
    }

    public String getUnrenderedContent(){
        return unrenderedContent;
    }
    public String getRenderedContent(){
        return renderedContent;
    }

    @Contract(" -> new")
    @NonNull ChatCompletionMessage asCompletionMessage(){
        if (renderedContent == null)
            throw new IllegalStateException("Called for completion message with no rendered content");
        return new ChatCompletionMessage(role, null, Macro.stripUnresolvedMacros(renderedContent));
    }

    PromptSection.InjectAtPosition getInjectionOrder(){
        return position;
    }

    boolean isChatHistorySection(){
        return isChatHistorySection;
    }

    void injectAtDetectedMacros(MacroManager macroManager){
        this.renderedContent = unrenderedContent;

        boolean replaced = true;
        while (replaced) {
            replaced = false;
            for (Macro macro : macroManager.getMacros()){
                Optional<String> toInject = macroManager.renderMacro(macro);
                if (toInject.isPresent()) {
                    Macro.ReplacementResult result = macro.replaceAt(renderedContent, toInject.get());
                    this.renderedContent = result.newContent();
                    replaced = result.injected();
                }
            }
        }
    }


}
