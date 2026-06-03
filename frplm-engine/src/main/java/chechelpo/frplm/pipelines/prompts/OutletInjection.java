package chechelpo.frplm.pipelines.prompts;

import chechelpo.frplm.domain.prompts.section.DefaultSections;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionMessage;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static chechelpo.frplm.domain.lorebook.outlet.StandardOutlet.UNRESOLVED_MACRO_INLINE;
import static chechelpo.frplm.domain.lorebook.outlet.StandardOutlet.UNRESOLVED_MACRO_LINE;
import static chechelpo.frplm.pipelines.prompts.EntryEvaluator.renderEntries;

final class OutletInjection {
    private OutletInjection() {}

    public record DetectedOutlet(int segmentIndex, int charOffset) {}

    public record OutletsOfSections(
            PromptSectionRecord section,
            List<IntObjectPair<DetectedOutlet>> outlets
    ) {}

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // INJECTION
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * @param outletsOfSections map <section
     * @param entriesToInject          map < outletID -> Entry  >
     * @param chatHistory       previous messages, injected if the standard chat history section is found
     * @return the chatCompletionMessages, ready to be injected into a prompt
     */
    @CheckReturnValue
    public static @NotNull List<ChatCompletionMessage> injectAndCreateRequest(
            @NotNull List<OutletsOfSections> outletsOfSections,
            @NotNull Map<Integer, List<EntryRecord>> entriesToInject,
            @NotNull List<MessagesRecord> chatHistory
    ) {
        List<ChatCompletionMessage> renderedMessages =
                new ArrayList<>(outletsOfSections.size() + chatHistory.size());

        for (OutletsOfSections outletSection : outletsOfSections) {
            PromptSectionRecord section = outletSection.section();

            if (DefaultSections.CHAT_HISTORY.sectionID == section.getSectionId()) {
                renderedMessages.addAll(render(chatHistory));
                continue;
            }

            String renderedContent = stripUnresolvedMacros(
                    injectEntries(
                            section.getContent(),
                            outletSection.outlets(),
                            entriesToInject
                    )
            );

            renderedMessages.add(
                    new ChatCompletionMessage(
                            ChatCompletionRole.fromWireValue(section.getRole()),
                            renderedContent
                    ));
        }

        return renderedMessages;
    }

    private static @NotNull String injectEntries(
            @NotNull String content,
            @NotNull List<IntObjectPair<DetectedOutlet>> detectedOutlets,
            @NotNull Map<Integer, List<EntryRecord>> toInject
    ) {
        if (detectedOutlets.isEmpty() || toInject.isEmpty()) {
            return content;
        }

        List<PendingInjection> pendingInjections = new ArrayList<>();

        for (IntObjectPair<DetectedOutlet> detectedOutlet : detectedOutlets) {
            int outletID = detectedOutlet.firstInt();
            DetectedOutlet location = detectedOutlet.second();

            List<EntryRecord> entries = toInject.get(outletID);

            if (entries == null || entries.isEmpty()) {
                continue;
            }

            int absoluteStart = absoluteOffsetOf(content, location);
            int absoluteEnd = outletMarkerEnd(content, absoluteStart);

            pendingInjections.add(new PendingInjection(
                    absoluteStart,
                    absoluteEnd,
                    renderEntries(entries, null, null)
            ));
        }

        if (pendingInjections.isEmpty()) {
            return content;
        }

        pendingInjections.sort(
                Comparator.comparingInt(PendingInjection::start).reversed()
        );

        StringBuilder rendered = new StringBuilder(content);

        for (PendingInjection injection : pendingInjections) {
            rendered.replace(
                    injection.start(),
                    injection.end(),
                    injection.content()
            );
        }

        return rendered.toString();
    }

    private record PendingInjection(
            int start,
            int end,
            @NotNull String content
    ) {
    }

    @Contract(pure = true)
    private static List<ChatCompletionMessage> render(
            @NotNull List<MessagesRecord> toInject
    ) {
        return toInject.stream()
                .map(message ->
                        new ChatCompletionMessage(
                                ChatCompletionRole.fromWireValue(message.getRole()),
                                message.getContent()
                        ))
                .toList();
    }

    @Contract(pure = true)
    private static int absoluteOffsetOf(
            @NotNull String text,
            @NotNull DetectedOutlet location
    ) {
        int segmentIndex = 0;
        int segmentStart = 0;

        while (segmentIndex < location.segmentIndex() && segmentStart < text.length()) {
            int segmentEnd = segmentStart;

            while (
                    segmentEnd < text.length()
                            && text.charAt(segmentEnd) != '\n'
                            && text.charAt(segmentEnd) != '\r'
            ) {
                segmentEnd++;
            }

            if (segmentEnd == text.length()) {
                return text.length();
            }

            if (
                    text.charAt(segmentEnd) == '\r'
                            && segmentEnd + 1 < text.length()
                            && text.charAt(segmentEnd + 1) == '\n'
            ) {
                segmentStart = segmentEnd + 2;
            } else {
                segmentStart = segmentEnd + 1;
            }

            segmentIndex++;
        }

        return Math.min(segmentStart + location.charOffset(), text.length());
    }

    @Contract(pure = true)
    private static int outletMarkerEnd(
            @NotNull String text,
            int markerStart
    ) {
        if (
                markerStart < 0
                        || markerStart + 1 >= text.length()
                        || text.charAt(markerStart) != '{'
                        || text.charAt(markerStart + 1) != '{'
        ) {
            return markerStart;
        }

        int closing = text.indexOf("}}", markerStart + 2);

        if (closing == -1) {
            return markerStart;
        }

        return closing + 2;
    }

    @Contract(pure = true)
    private static @NotNull String stripUnresolvedMacros(@NotNull String content) {
        String withoutMacroOnlyLines = UNRESOLVED_MACRO_LINE
                .matcher(content)
                .replaceAll("");

        return UNRESOLVED_MACRO_INLINE
                .matcher(withoutMacroOnlyLines)
                .replaceAll("");
    }
}