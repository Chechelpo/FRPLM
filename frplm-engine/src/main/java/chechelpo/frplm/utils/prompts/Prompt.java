package chechelpo.frplm.utils.prompts;

import chechelpo.frplm.extensions.api.session.ChatMessage;
import chechelpo.frplm.extensions.api.standalone.EntrySnapshot;
import chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import chechelpo.frplm.extensions.api.standalone.PromptSectionSnapshot;
import chechelpo.frplm.extensions.api.utils.DetectedOutlet;
import chechelpo.frplm.extensions.api.utils.MessagePrompt;
import chechelpo.frplm.extensions.api.utils.PromptBuilder;
import chechelpo.frplm.extensions.implementations.standalone.EntryImpl;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionMessage;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.stream.Collectors;

public record Prompt (
        LorebookSnapshot[] usedLorebooks,
        EntrySnapshot[] activatedEntries,
        ChatCompletionRequest renderedRequest
) implements MessagePrompt {
    public enum Phase {PRE_RENDER, RENDERED}

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static class Builder implements PromptBuilder {
        private Phase phase = Phase.PRE_RENDER;

        private List<LorebookSnapshot> lorebooks = new ArrayList<>(10);
        private ChatCompletionRequest.Builder requestBuilder = ChatCompletionRequest.builder();
        /**
         * Indexes of sections within the prompt
         */
        private IntList sectionIndexes = new IntArrayList(10);

        private Int2ObjectMap<KeywordDetection.DetectedKeyword> detectedKeywords;
        /**
         * at index -> list of {@link DetectedOutlet}
         */
        private Int2ObjectMap<List<DetectedOutlet>> detectedOutlets;
        /**
         * outletID -> list of ACTIVE entries
         */
        private Int2ObjectMap<List<EntryRecord>> activeEntriesByOutlet = new Int2ObjectArrayMap<>(100);

        private Builder() {}

        private void assertPreRender() {
            if (this.phase != Phase.PRE_RENDER) throw new IllegalArgumentException("This is not a pre render phase");
        }

        @Override
        public @UnmodifiableView List<LorebookSnapshot> getLorebooks() {
            return Collections.unmodifiableList(this.lorebooks);
        }

        public Builder addLorebook(LorebookSnapshot lorebook) {
            assertPreRender();
            lorebooks.add(lorebook);
            return this;
        }

        public Builder addLorebooks(List<LorebookSnapshot> lorebooks) {
            assertPreRender();
            this.lorebooks.addAll(lorebooks);
            return this;
        }

        public Builder addLorebooks(LorebookSnapshot... lorebookSnapshots) {
            assertPreRender();
            lorebooks.addAll(List.of(lorebookSnapshots));
            return this;
        }

        public Builder appendSections(List<PromptSectionSnapshot> sections) {
            if (sections == null) throw new IllegalArgumentException("Null sections");
            sections.forEach(section -> appendAsSection(section.asCompletionMessage()));
            return this;
        }

        public Builder appendSection(PromptSectionSnapshot section) {
            if (section == null) throw new IllegalArgumentException("Null sections");
            this.appendAsSection(section.asCompletionMessage());
            return this;
        }

        public Builder appendAsSection(ChatCompletionMessage section) {
            sectionIndexes.add(requestBuilder.getMessages().size());
            requestBuilder.append(section);
            return this;
        }

        public Builder append(@NotNull ChatMessage message) {
            requestBuilder.append(message.asChatCompletion());
            return this;
        }

        public Builder appendAll(@NotNull List<ChatMessage> chatHistory) {
            requestBuilder.appendAll(chatHistory.stream().map(ChatMessage::asChatCompletion).toList());
            return this;
        }

        @Override
        public Builder insertAt(int depth, ChatCompletionMessage message) {
            assertPreRender();

            for (int i = 0; i < sectionIndexes.size(); i++) {
                int sectionIndex = sectionIndexes.getInt(i);

                if (sectionIndex >= depth) {
                    sectionIndexes.set(i, sectionIndex + 1);
                }
            }

            requestBuilder.insertAt(depth, message);
            return this;
        }

        public Prompt build(ExtensionContext context, String modelId){
            if (this.phase != Phase.RENDERED) throw new IllegalArgumentException("This prompt is not rendered yet");
            List<EntryRecord> activated = new ArrayList<>();
            for (List<EntryRecord> entries : activeEntriesByOutlet.values()) activated.addAll(entries);
            return new Prompt(
                    lorebooks.toArray(new LorebookSnapshot[0]),
                    activated.stream()
                            .map(record -> new EntryImpl(record, context))
                            .map(EntrySnapshot.class::cast)
                            .toArray(EntrySnapshot[]::new),
                    requestBuilder.modelID(modelId).build()
            );
        }

        public Builder render(@NotNull ExtensionContext context) {
            IntSet lorebookIds = IntSetFactory.ofValues(
                    lorebooks.stream()
                            .map(book -> book.reference().id())
                            .toList()
            );
            detectOutletsINPrompt(lorebookIds, context);

            List<IntObjectPair<String>> allKeywordsOfLorebooks = context.entryKeywords().getKeywords(lorebookIds);
            recursiveFetchActivatedEntries(lorebookIds, allKeywordsOfLorebooks, context);
            for (var outletPlace : detectedOutlets.int2ObjectEntrySet()){
                int toInjectIndex = outletPlace.getIntKey();
                ChatCompletionMessage original = requestBuilder.getAt(toInjectIndex);
                String newContent = OutletInjection.inject(original.content(), outletPlace.getValue(), activeEntriesByOutlet);

                requestBuilder.setAt(toInjectIndex, new ChatCompletionMessage(original.role(), newContent));
            }
            this.phase = Phase.RENDERED;

            return this;
        }


        @Contract(mutates = "this")
        private void detectOutletsINPrompt(IntSet lorebookIds, ExtensionContext context) {
            if (sectionIndexes.isEmpty()) return;
            IntSet uniqueDetectedOutletIDs = IntSetFactory.ofLength(sectionIndexes.size() * 2);
            Int2ObjectMap<List<DetectedOutlet>> promptsAndDetectedOutlets = new Int2ObjectArrayMap<>(sectionIndexes.size());

            IntObjectPair<String>[] allOutlets = context.outlets().getOutlets(lorebookIds);
            for (int index : sectionIndexes) {
                List<DetectedOutlet> detectedInSection = OutletDetection.getDetectedOutlets(
                        allOutlets,
                        requestBuilder.getAt(index).content()
                );
                uniqueDetectedOutletIDs.addAll(detectedInSection.stream().map(DetectedOutlet::outletId).toList());

                promptsAndDetectedOutlets.put(index, detectedInSection);
            }
            uniqueDetectedOutletIDs.intStream().forEach(outletId -> activeEntriesByOutlet.put(outletId, new ArrayList<>()));

            this.detectedOutlets = promptsAndDetectedOutlets;
        }

        @Contract(mutates = "this")
        private void recursiveFetchActivatedEntries(
                IntSet lorebookIds,
                List<IntObjectPair<String>> allKeywords,
                @NotNull ExtensionContext context
        ) {
            this.detectedKeywords = KeywordDetection.detectParallelIn(
                    allKeywords,
                    requestBuilder.getMessages().stream().map(ChatCompletionMessage::content).collect(Collectors.toList())
            );

            Set<EntryRecord> detectedEntries = new HashSet<>(100);
            int recursionSteps = 10;
            for (int i = 0; i < recursionSteps; i++) {
                int detectedKeywordsSize = this.detectedKeywords.size();
                filterActivated(
                        detectedEntries,
                        context.entries().getEntriesWith(
                                lorebookIds,
                                this.detectedKeywords.keySet()
                        ),
                        allKeywords,
                        i,
                        context
                );
                if (detectedKeywords.size() == detectedKeywordsSize) break;
            }

        }

        /**
         * Processes not yet activated entries. Mutates {@link #detectedKeywords} based on the new activated entries keywords.
         *
         * @param processedEntries entries previously touched by this function
         * @param entries          all entries, including processed
         * @param allKeywords      all keywords detected throughout the entries lorebooks
         * @param recursionStep    a counter of how many times this function has been called
         * @param context          engine context
         */
        @Contract(mutates = "this, param1")
        private void filterActivated(
                Set<EntryRecord> processedEntries,
                @NotNull List<EntryRecord> entries,
                List<IntObjectPair<String>> allKeywords,
                int recursionStep,
                ExtensionContext context
        ) {
            for (EntryRecord entryRecord : entries) {
                if (processedEntries.contains(entryRecord)) continue;
                processedEntries.add(entryRecord);

                int deepestKeywordDepth = getDeepestKeywordDepth(context, entryRecord);

                boolean isActive = EntryEvaluator.activates(entryRecord, recursionStep, deepestKeywordDepth);
                if (isActive) {
                    if (!activeEntriesByOutlet.containsKey(entryRecord.getOutlet()))
                        throw new IllegalStateException("Outlet " + entryRecord.getOutlet() + " does not exist in active entries by outlet");
                    activeEntriesByOutlet.get(entryRecord.getOutlet()).add(entryRecord);

                    if (!entryRecord.getPreventFurtherRecursion()) {
                        KeywordDetection.DetectedKeyword detectedKeyword = new KeywordDetection.DetectedKeyword(-1, 0);
                        KeywordDetection.detectKeywordsIn(allKeywords, entryRecord.getContent())
                                .forEach(keywordID -> detectedKeywords.put(keywordID, detectedKeyword));
                    }
                }
            }
        }

        private int getDeepestKeywordDepth(ExtensionContext context, EntryRecord entryRecord) {
            Set<Integer> keywords = context.entryKeywords().keywordIDsOfEntry(
                    entryRecord.getLorebookId(),
                    entryRecord.getEntryId()
            );

            int deepestKeywordDepth = 0;
            for (int keywordID : keywords) {
                if (!detectedKeywords.containsKey(keywordID)) continue;
                if (detectedKeywords.get(keywordID).atDepth() > deepestKeywordDepth)
                    deepestKeywordDepth = detectedKeywords.get(keywordID).atDepth();
            }
            return deepestKeywordDepth;
        }
    }
}
