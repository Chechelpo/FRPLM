package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.utils.matching.FlexiblePattern;
import io.github.chechelpo.frplm.utils.matching.Macro;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.utils.matching.ReplacementTarget;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

final class MacroManager {
    private static final int INITIAL_MACRO_CAPACITY = 20;
    private static final int INITIAL_PER_MACRO_CAPACITY = 20;

    private final Map<ReplacementTarget, List<String>> injections = new HashMap<>(INITIAL_MACRO_CAPACITY);
    private final Map<ReplacementTarget, String> rendered = new HashMap<>(INITIAL_MACRO_CAPACITY);
    private final OutletManagerImpl outletManager;
    private boolean askedToRender = false;

    MacroManager(OutletManagerImpl outletManager) {
        this.outletManager = outletManager;
    }

    List<String> getAtMacro(String macro){
        return getAtMacro(new Macro(macro));
    }
    List<String> getAtMacro(Macro macro){
        return injections.get(macro);
    }

    private void requireNotRender(){
        if (askedToRender) throw new IllegalStateException("Asked to inject after rendering a target");
    }

    void addEntries(List<EntryRecord> entries) {
        requireNotRender();
        Objects.requireNonNull(entries);
        Objects.requireNonNull(outletManager);
        entries.forEach(entry -> {
                Objects.requireNonNull(entry);
                appendAtMacro(outletManager.getOutletOf(entry), entry.getContent());
            }
        );
    }

    void replaceAt(String regex, String content){
        replaceAt(new FlexiblePattern(regex), content);
    }
    void replaceAt(FlexiblePattern pattern, String content){
        requireNotRender();
        injections.computeIfAbsent(
                pattern,
                ignored -> new ArrayList<>()
        ).add(content);
    }

    void appendAtMacro(String macro, String content){
        appendAtMacro(new Macro(macro), content);
    }
    void appendAtMacro(Macro macro, String content) {
        injectAtMacro(
                macro,
                content,
                Integer.MAX_VALUE
        );
    }

    void prependAtMacro(String macro, String content) {
        prependAtMacro(new Macro(macro), content);
    }
    void prependAtMacro(Macro macro, String content) {
        injectAtMacro(
                macro,
                content,
                0
        );
    }

    void injectAtMacro(String macro, String content, int atIndex){
        injectAtMacro(new Macro(macro), content, atIndex);
    }
    void injectAtMacro(Macro macro, String content, int atIndex) {
        requireNotRender();
        if (atIndex == -1) {
            appendAtMacro(macro, content);
            return;
        }
        Objects.requireNonNull(macro);
        Objects.requireNonNull(content);

        List<String> previousContents = injections.computeIfAbsent(
                macro,
                ignored -> new ArrayList<>(INITIAL_PER_MACRO_CAPACITY)
        );
        if (atIndex < 0)
            atIndex = Math.max(0, previousContents.size() + atIndex);

        previousContents.add(
                Math.min(previousContents.size(), atIndex),
                content
        );
    }

    @Contract(pure = true)
    @NonNull Set<ReplacementTarget> getTargets(){
        return injections.keySet();
    }

    @NotNull Optional<String> renderTarget(String macroName){
        return renderTarget(new Macro(macroName));
    }

    @NonNull Optional<String> renderTarget(ReplacementTarget target) {
        Objects.requireNonNull(target, "Requested target to render is null");
        askedToRender = true;
        return Optional.ofNullable(rendered.computeIfAbsent(
                target,
                newMacro -> {
                    if (!injections.containsKey(newMacro)) return null;
                    List<String> contentToRender = injections.get(newMacro);

                    StringBuilder builder = new StringBuilder(contentToRender.stream().mapToInt(String::length).sum());
                    contentToRender.forEach(builder::append);

                    return builder.toString();
                }
        ));
    }

    String replaceTargets(String content) {
        Objects.requireNonNull(content);

        String result = content;

        boolean replaced;
        do {
            replaced = false;

            for (ReplacementTarget target : getTargets()) {
                Optional<String> replacement = renderTarget(target);

                if (replacement.isEmpty()) {
                    continue;
                }

                ReplacementTarget.ReplacementResult replacementResult =
                        target.replaceAt(result, replacement.get());

                String newContent = replacementResult.newContent();

                replaced |= !newContent.equals(result);

                result = newContent;
            }
        } while (replaced);

        return result;
    }
}
