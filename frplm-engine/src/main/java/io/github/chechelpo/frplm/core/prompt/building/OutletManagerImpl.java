package io.github.chechelpo.frplm.core.prompt.building;

import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.api.prompts.OutletManager;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.utils.macros.Macro;
import io.github.chechelpo.frplm.utils.macros.Outlet;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

final class OutletManagerImpl implements OutletManager {
    private static final Logger log = (Logger) LoggerFactory.getLogger("Outlet Manager");

    private final OutletService outletService;
    private final LorebookManagerImpl lorebookManager;

    /**
     * (lorebookId, outletId) -> override outlet
     */
    private final HashMap<IntIntPair, Macro> lorebookOutletOverrides = new HashMap<>();
    /**
     * (lorebookId) -> override outletId
     */
    private final Int2ObjectMap<Macro> lorebookOverrides = new Int2ObjectArrayMap<>();
    /**
     * outletId -> overrideOutlet
     */
    private final Int2ObjectMap<Macro> globalOutletOverrides = new Int2ObjectArrayMap<>();

    OutletManagerImpl(OutletService outletService, LorebookManagerImpl lorebooks) {
        this.outletService = outletService;
        this.lorebookManager = lorebooks;
    }

    @Override
    public OverrideResult overrideLorebookOutlet(@NonNull LorebookSnapshot targetLorebook, String targetOutlet, String newOutlet) {
        if (!lorebookManager.containsLorebook(targetLorebook))
            return OverrideResult.TARGET_LOREBOOK_DOES_NOT_EXIST;

        int targetLorebookId = targetLorebook.asReference().id();
        Optional<Integer> previousOutletId = outletService.getOutletID(targetOutlet);
        if (previousOutletId.isEmpty()){
            return OverrideResult.TARGET_OUTLET_DOES_NOT_EXIST;
        }

        lorebookOutletOverrides.put(IntIntPair.of(targetLorebookId, previousOutletId.get()), new Macro(newOutlet));
        return OverrideResult.SUCCESS;
    }

    @Override
    public OverrideResult overrideAllLorebookOutlets(LorebookSnapshot targetLorebook, String newOutletName) {
        if (!lorebookManager.containsLorebook(targetLorebook))
            return OverrideResult.TARGET_LOREBOOK_DOES_NOT_EXIST;

        int lorebookId = targetLorebook.asReference().id();
        if (lorebookOverrides.containsKey(lorebookId)){
            log.debug("Lorebook {} already has an active lorebook-wide override: {}", targetLorebook.getName(), newOutletName);
            return OverrideResult.ALREADY_OVERRIDDEN;
        }

        lorebookOverrides.put(lorebookId, new Macro(newOutletName));
        return OverrideResult.SUCCESS;
    }

    @Override
    public OverrideResult overrideOutlet(String targetOutlet, String newOutlet) {
        Optional<Integer> previousOutlet = outletService.getOutletID(targetOutlet);
        if (previousOutlet.isEmpty()) return OverrideResult.TARGET_OUTLET_DOES_NOT_EXIST;

        globalOutletOverrides.put((int) previousOutlet.get(), new Macro(newOutlet));

        return OverrideResult.SUCCESS;
    }

    /**
     * Retrieves the outlet name for the specified entry record, checking overrides in the following order:
     * <pre>
     *      1. Per outlet lorebook overrides
     *      2. Lorebook-wide overrides
     *      3. Global outlet overrides
     * </pre>
     * If no override is found, it falls back to this entry's outlet via {@link OutletService#getOutletName(int)}
     *
     * @param entry the entry record for which to retrieve the outlet id; must not be null
     * @return the non-null outlet name associated with the entry
     * @throws NullPointerException if the entry is null
     * @throws EntityNotFound       if the entry's outlet ID has no backing outlet name
     */
    @NonNull
    Macro getOutletOf(EntryRecord entry) {
        Objects.requireNonNull(entry);

        int lorebookId = entry.getLorebookId();
        int outletId = entry.getOutlet();

        Macro override = lorebookOutletOverrides.get(IntIntPair.of(lorebookId, outletId));
        if (override != null) return override;


        override = lorebookOverrides.get(lorebookId);
        if (override != null) return override;


        override = globalOutletOverrides.get(outletId);
        if (override != null) return override;


        String outletName = outletService.getOutletName(outletId)
                .orElseThrow(() -> {
                    log.error(
                            "Couldn't find outlet name for entry:\n{}",
                            entry
                    );

                    return new EntityNotFound(
                            "Entry %s outlet ID %d has no backing outlet name"
                                    .formatted(entry.getName(), outletId),
                            Severity.SYSTEM
                    );
                });

        return new Outlet(outletName);
    }


}
