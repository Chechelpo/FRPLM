package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.extensions.api.prompts.PromptSection;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionMessage;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.stream.Collectors;

final class PromptRenderer {

    private final List<SectionManager> sections = new ArrayList<>();
    private final List<ChatMessage> chatHistory;

    public PromptRenderer(List<ChatMessage> chatHistory, @NonNull List<SectionManager> initialSections){
        this.chatHistory = chatHistory;
        sections.addAll(initialSections);
        sections.stream().filter(SectionManager::isChatHistorySection).findAny()
                .orElseThrow(() -> new IllegalStateException("There's no chat history section in this prompt"));
    }
    public PromptRenderer(List<SectionManager> initialSections){
        chatHistory = null;
        this.sections.addAll(initialSections);
    }

    List<ChatMessage> getChatHistory(){
        return chatHistory;
    }

    void addSection(@NonNull ChatCompletionMessage message, PromptSection.InjectAtPosition injectionOrder){
        sections.add(new SectionManager(message.content(), message.role(), injectionOrder));
    }

    void appendSection(SectionManager section){
        sections.add(section);
    }

    List<ChatCompletionMessage> render(MacroManager macroManager) {
        sections.forEach(
                section -> section.injectAtDetectedTargets(macroManager)
        );

        List<ChatCompletionMessage> orderedMessages =
                new ArrayList<>(sections.size() + chatHistory.size());


        Map<Integer, List<SectionManager>> atDepthSections =
                sections.stream()
                        .filter(sec -> sec.getInjectionOrder() instanceof PromptSection.InjectAtPosition.AtDepth)
                        .collect(Collectors.groupingBy(
                                sec -> ((PromptSection.InjectAtPosition.AtDepth) sec.getInjectionOrder()).atDepth(),
                                TreeMap::new,
                                Collectors.toList()
                        ));

        sections.stream()
                .filter(sec -> sec.getInjectionOrder() instanceof PromptSection.InjectAtPosition.Relative)
                .sorted(Comparator.comparingInt(sec ->
                        ((PromptSection.InjectAtPosition.Relative) sec.getInjectionOrder()).number()
                ))
                .forEach(sec -> {
                    if (sec.isChatHistorySection()) {
                        appendChatHistoryWithDepthInjections(
                                orderedMessages.size(),
                                orderedMessages.size() + chatHistory.size(),
                                orderedMessages,
                                atDepthSections,
                                macroManager
                        );
                        return;
                    }

                    orderedMessages.add(sec.asCompletionMessage());
                });

        return orderedMessages;
    }

    private void appendChatHistoryWithDepthInjections(
            int chatHistoryStartOffset,
            int chatHistoryEndOffset,
            List<ChatCompletionMessage> orderedMessages,
            Map<Integer, List<SectionManager>> atDepthSectionsByIndex,
            MacroManager macroManager
    ) {
        chatHistory.forEach(chatMessage -> {
            ChatCompletionMessage message =
                    chatMessage.asChatCompletion();

            orderedMessages.add(
                    new ChatCompletionMessage(
                            message.role(),
                            message.reasoning(),
                            macroManager.replaceTargets(message.content())
                    )
            );
        });

        for (Map.Entry<Integer, List<SectionManager>> entry
                : atDepthSectionsByIndex.entrySet()) {

            int depth = Math.max(
                    chatHistoryStartOffset,
                    chatHistoryEndOffset - entry.getKey()
            );

            entry.getValue().forEach(
                    sec -> orderedMessages.add(
                            depth,
                            sec.asCompletionMessage()
                    )
            );
        }
    }
}
