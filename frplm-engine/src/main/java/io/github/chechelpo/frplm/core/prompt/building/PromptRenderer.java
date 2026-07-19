package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.extensions.api.prompts.PromptSection;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSectionEntitySnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionMessage;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.stream.Collectors;

final class PromptRenderer {

    private final Int2ObjectMap<List<String>> customInjections = new Int2ObjectArrayMap<>(30);
    private final Int2ObjectMap<String> renderedContent = new Int2ObjectArrayMap<>(20);
    private final List<SectionManager> sections = new ArrayList<>();
    private final List<ChatMessage> chatHistory;

    public PromptRenderer(List<ChatMessage> chatHistory, @NonNull List<SectionManager> initialSections){
        this.chatHistory = chatHistory;
        sections.addAll(initialSections);
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

    List<ChatCompletionMessage> render(LorebooksManager lorebooksManager) {
        sections.forEach(
                section -> section.injectEntriesAtDetectedOutlets(lorebooksManager, this)
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
                                atDepthSections
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
            Map<Integer, List<SectionManager>> atDepthSectionsByIndex
    ) {
        chatHistory.forEach(chatMessage ->  orderedMessages.add(chatMessage.asChatCompletion()));
        for (Map.Entry<Integer, List<SectionManager>> entry : atDepthSectionsByIndex.entrySet()){
            int depth = Math.max(chatHistoryStartOffset, chatHistoryEndOffset - entry.getKey());
            List<SectionManager> toInject = entry.getValue();
            toInject.forEach(sec -> orderedMessages.add(depth, sec.asCompletionMessage()));
        }
    }

    Optional<String> renderEligibleAtOutlet(
            int outletId,
            LorebooksManager lorebooksManager
    ) {
        return Optional.of(renderedContent.computeIfAbsent(outletId, key -> {
            List<EntryRecord> outletEntries =
                    lorebooksManager.getOf(key).orElse(List.of());

            List<String> injections =
                    customInjections.getOrDefault(key, List.of());

            int stringSize =
                    outletEntries.stream()
                            .map(EntryRecord::getContent)
                            .filter(content -> content != null && !content.isBlank())
                            .mapToInt(String::length)
                            .sum()
                            +
                            injections.stream()
                                    .filter(content -> content != null && !content.isBlank())
                                    .mapToInt(String::length)
                                    .sum()
                            +
                            Math.max(0, outletEntries.size() + injections.size() - 1);

            StringBuilder builder = new StringBuilder(stringSize);

            for (EntryRecord entry : outletEntries) {
                String content = entry.getContent();

                if (content == null || content.isBlank()) {
                    continue;
                }

                appendWithSeparator(builder, content);
            }

            for (String injection : injections) {
                if (injection == null || injection.isBlank()) {
                    continue;
                }

                appendWithSeparator(builder, injection);
            }

            return builder.toString();
        }));
    }
    private static void appendWithSeparator(StringBuilder builder, String content) {
        if (!builder.isEmpty()) {
            builder.append('\n');
        }

        builder.append(content);
    }
}
