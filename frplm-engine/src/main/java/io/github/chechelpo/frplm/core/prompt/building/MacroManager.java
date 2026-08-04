package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.utils.macros.Macro;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

final class MacroManager {
    private static final int INITIAL_MACRO_CAPACITY = 20;
    private static final int INITIAL_PER_MACRO_CAPACITY = 20;

    private final Map<Macro, List<String>> injections = new HashMap<>(INITIAL_MACRO_CAPACITY);
    private final Map<Macro, String> rendered = new HashMap<>(INITIAL_MACRO_CAPACITY);
    private final OutletManagerImpl outletManager;

    MacroManager(OutletManagerImpl outletManager) {
        this.outletManager = outletManager;
    }

    List<String> getAtMacro(String macro){
        return getAtMacro(new Macro(macro));
    }
    List<String> getAtMacro(Macro macro){
        return injections.get(macro);
    }

    void addEntries(List<EntryRecord> entries) {
        Objects.requireNonNull(entries);
        Objects.requireNonNull(outletManager);
        entries.forEach(entry -> {
                Objects.requireNonNull(entry);
                appendAtMacro(outletManager.getOutletOf(entry), entry.getContent());
            }
        );
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
        if (atIndex == -1) appendAtMacro(macro, content);
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
    @NonNull Set<Macro> getMacros(){
        return injections.keySet();
    }

    @NotNull Optional<String> renderMacro(String macroName){
        return renderMacro(new Macro(macroName));
    }
    @NonNull Optional<String> renderMacro(Macro macro) {
        Objects.requireNonNull(macro, "Requested macro to render is null");

        return Optional.ofNullable(rendered.computeIfAbsent(
                macro,
                newMacro -> {
                    if (!injections.containsKey(newMacro)) return null;
                    List<String> contentToRender = injections.get(newMacro);

                    StringBuilder builder = new StringBuilder(contentToRender.stream().mapToInt(String::length).sum());
                    contentToRender.forEach(builder::append);

                    return builder.toString();
                }
        ));
    }
}
