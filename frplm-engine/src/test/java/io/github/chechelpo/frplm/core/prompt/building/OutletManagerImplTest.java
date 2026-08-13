package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletServiceTestFactory;
import io.github.chechelpo.frplm.extensions.api.prompts.OutletManager;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.utils.matching.Macro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class OutletManagerImplTest {
    private OutletService outletService;
    private LorebookManagerImpl lorebookManager;
    private OutletManagerImpl outletManager;

    @BeforeEach
    void setUp(){
        outletService = OutletServiceTestFactory.mockService();
        lorebookManager = mock(LorebookManagerImpl.class);
        outletManager = new io.github.chechelpo.frplm.core.prompt.building.OutletManagerImpl(outletService, lorebookManager);
    }

    @Nested
    class OverrideResults{
        @Test
        void overrideLorebookOutletReturnsTargetLorebookDoesNotExist() {
            LorebookSnapshot lorebook = mock(LorebookSnapshot.class);

            when(lorebookManager.containsLorebook(lorebook))
                    .thenReturn(false);

            OutletManager.OverrideResult result =
                    outletManager.overrideLorebookOutlet(
                            lorebook,
                            "oldOutlet",
                            "newOutlet"
                    );

            assertEquals(
                    OutletManager.OverrideResult.TARGET_LOREBOOK_DOES_NOT_EXIST,
                    result
            );

            verify(outletService, never()).getOutletID(anyString());
            verify(lorebook, never()).asReference();
        }
        @Test
        void overrideLorebookOutletReturnsTargetOutletDoesNotExist() {
            int lorebookId = 10;
            LorebookSnapshot lorebook = mockLorebook(
                    lorebookId,
                    "Test lorebook"
            );

            when(outletService.getOutletID("missingOutlet"))
                    .thenReturn(Optional.empty());

            OutletManager.OverrideResult result =
                    outletManager.overrideLorebookOutlet(
                            lorebook,
                            "missingOutlet",
                            "newOutlet"
                    );

            assertEquals(
                    OutletManager.OverrideResult.TARGET_OUTLET_DOES_NOT_EXIST,
                    result
            );
        }
        @Test
        void overrideLorebookOutletReturnsSuccess() {
            int lorebookId = 10;
            int outletId = 20;

            LorebookSnapshot lorebook = mockLorebook(
                    lorebookId,
                    "Test lorebook"
            );

            when(outletService.getOutletID("oldOutlet"))
                    .thenReturn(Optional.of(outletId));

            OutletManager.OverrideResult result =
                    outletManager.overrideLorebookOutlet(
                            lorebook,
                            "oldOutlet",
                            "newOutlet"
                    );

            assertEquals(
                    OutletManager.OverrideResult.SUCCESS,
                    result
            );

            EntryRecord entry = createEntry(lorebookId, outletId);

            assertEquals(
                    new Macro("newOutlet"),
                    outletManager.getOutletOf(entry)
            );
        }
        @Test
        void overrideAllLorebookOutletsReturnsTargetLorebookDoesNotExist() {
            LorebookSnapshot lorebook = mock(LorebookSnapshot.class);

            when(lorebookManager.containsLorebook(lorebook))
                    .thenReturn(false);

            OutletManager.OverrideResult result =
                    outletManager.overrideAllLorebookOutlets(
                            lorebook,
                            "newOutlet"
                    );

            assertEquals(
                    OutletManager.OverrideResult.TARGET_LOREBOOK_DOES_NOT_EXIST,
                    result
            );

            verify(lorebook, never()).asReference();
        }
        @Test
        void overrideAllLorebookOutletsReturnsSuccess() {
            int lorebookId = 10;

            LorebookSnapshot lorebook = mockLorebook(
                    lorebookId,
                    "Test lorebook"
            );

            OutletManager.OverrideResult result =
                    outletManager.overrideAllLorebookOutlets(
                            lorebook,
                            "newOutlet"
                    );

            assertEquals(
                    OutletManager.OverrideResult.SUCCESS,
                    result
            );

            EntryRecord entry = createEntry(lorebookId, 20);

            assertEquals(
                    new Macro("newOutlet"),
                    outletManager.getOutletOf(entry)
            );
        }
        @Test
        void overrideAllLorebookOutletsReturnsAlreadyOverridden() {
            int lorebookId = 10;

            LorebookSnapshot lorebook = mockLorebook(
                    lorebookId,
                    "Test lorebook"
            );

            assertEquals(
                    OutletManager.OverrideResult.SUCCESS,
                    outletManager.overrideAllLorebookOutlets(
                            lorebook,
                            "firstOverride"
                    )
            );

            OutletManager.OverrideResult secondResult =
                    outletManager.overrideAllLorebookOutlets(
                            lorebook,
                            "secondOverride"
                    );

            assertEquals(
                    OutletManager.OverrideResult.ALREADY_OVERRIDDEN,
                    secondResult
            );
        }
        @Test
        void alreadyOverriddenResultPreservesOriginalLorebookOverride() {
            int lorebookId = 10;
            int outletId = 20;

            LorebookSnapshot lorebook = mockLorebook(
                    lorebookId,
                    "Test lorebook"
            );

            assertEquals(
                    OutletManager.OverrideResult.SUCCESS,
                    outletManager.overrideAllLorebookOutlets(
                            lorebook,
                            "firstOverride"
                    )
            );

            assertEquals(
                    OutletManager.OverrideResult.ALREADY_OVERRIDDEN,
                    outletManager.overrideAllLorebookOutlets(
                            lorebook,
                            "secondOverride"
                    )
            );

            EntryRecord entry = createEntry(lorebookId, outletId);

            assertEquals(
                    new Macro("firstOverride"),
                    outletManager.getOutletOf(entry)
            );
        }
        @Test
        void overrideOutletReturnsTargetOutletDoesNotExist() {
            when(outletService.getOutletID("missingOutlet"))
                    .thenReturn(Optional.empty());

            OutletManager.OverrideResult result =
                    outletManager.overrideOutlet(
                            "missingOutlet",
                            "newOutlet"
                    );

            assertEquals(
                    OutletManager.OverrideResult.TARGET_OUTLET_DOES_NOT_EXIST,
                    result
            );
        }
        @Test
        void overrideOutletReturnsSuccess() {
            int outletId = 20;

            when(outletService.getOutletID("oldOutlet"))
                    .thenReturn(Optional.of(outletId));

            OutletManager.OverrideResult result =
                    outletManager.overrideOutlet(
                            "oldOutlet",
                            "newOutlet"
                    );

            assertEquals(
                    OutletManager.OverrideResult.SUCCESS,
                    result
            );

            EntryRecord entry = createEntry(10, outletId);

            assertEquals(
                    new Macro("newOutlet"),
                    outletManager.getOutletOf(entry)
            );
        }
    }

    @Nested
    class OutletManagerEntryOverrides{
        @Test
        void overrideLorebook(){
            String targetOutlet = "oldOutlet";
            int targetOutletId = 1;
            String newOutlet = "newOutlet";

            LorebookSnapshot snapshot = mock(LorebookSnapshot.class);
            int lorebookId = 1;
            when(snapshot.asReference()).thenReturn(new LorebookSnapshot.Reference(lorebookId));
            when(lorebookManager.containsLorebook(snapshot)).thenReturn(true);
            when(outletService.getOutletID(targetOutlet)).thenReturn(Optional.of(targetOutletId));

            OutletManager.OverrideResult result = outletManager.overrideLorebookOutlet(snapshot, targetOutlet, newOutlet);
            assertEquals(OutletManager.OverrideResult.SUCCESS, result);

            EntryRecord testEntry = new EntryRecord();
            testEntry.setLorebookId(lorebookId);
            testEntry.setOutlet(targetOutletId);

            Macro actualMacro = outletManager.getOutletOf(testEntry);
            Macro expectedMacro = new Macro(newOutlet);

            assertEquals(expectedMacro, actualMacro);
        }

        @Test
        void lorebookWideOverrideIsApplied() {
            int lorebookId = 10;
            int outletId = 20;

            String replacementOutlet = "lorebookReplacement";

            LorebookSnapshot lorebook = mockLorebook(
                    lorebookId,
                    "Test lorebook"
            );

            OutletManager.OverrideResult result =
                    outletManager.overrideAllLorebookOutlets(
                            lorebook,
                            replacementOutlet
                    );

            assertEquals(
                    OutletManager.OverrideResult.SUCCESS,
                    result
            );

            EntryRecord entry = createEntry(
                    lorebookId,
                    outletId
            );

            assertEquals(
                    new Macro(replacementOutlet),
                    outletManager.getOutletOf(entry)
            );

            verify(outletService, never()).getOutletName(outletId);
        }

        @Test
        void globalOutletOverrideIsApplied() {
            int lorebookId = 10;
            int outletId = 20;

            String originalOutlet = "original";
            String replacementOutlet = "globalReplacement";

            when(outletService.getOutletID(originalOutlet))
                    .thenReturn(Optional.of(outletId));

            OutletManager.OverrideResult result =
                    outletManager.overrideOutlet(
                            originalOutlet,
                            replacementOutlet
                    );

            assertEquals(
                    OutletManager.OverrideResult.SUCCESS,
                    result
            );

            EntryRecord entry = createEntry(
                    lorebookId,
                    outletId
            );

            assertEquals(
                    new Macro(replacementOutlet),
                    outletManager.getOutletOf(entry)
            );

            verify(outletService, never()).getOutletName(outletId);
        }

        @Test
        void overridesFollowSpecificThenLorebookThenGlobalPriority() {
            int targetLorebookId = 10;
            int otherLorebookId = 11;
            int targetOutletId = 20;
            int otherOutletId = 21;

            String targetOutlet = "targetOutlet";
            String otherOutlet = "otherOutlet";

            LorebookSnapshot targetLorebook = mockLorebook(
                    targetLorebookId,
                    "Target lorebook"
            );

            when(outletService.getOutletID(targetOutlet))
                    .thenReturn(Optional.of(targetOutletId));

            when(outletService.getOutletID(otherOutlet))
                    .thenReturn(Optional.of(otherOutletId));

            assertEquals(
                    OutletManager.OverrideResult.SUCCESS,
                    outletManager.overrideOutlet(
                            targetOutlet,
                            "globalOverride"
                    )
            );

            assertEquals(
                    OutletManager.OverrideResult.SUCCESS,
                    outletManager.overrideOutlet(
                            otherOutlet,
                            "otherGlobalOverride"
                    )
            );

            assertEquals(
                    OutletManager.OverrideResult.SUCCESS,
                    outletManager.overrideAllLorebookOutlets(
                            targetLorebook,
                            "lorebookOverride"
                    )
            );

            assertEquals(
                    OutletManager.OverrideResult.SUCCESS,
                    outletManager.overrideLorebookOutlet(
                            targetLorebook,
                            targetOutlet,
                            "specificOverride"
                    )
            );

            /*
             * Has all three applicable overrides:
             * specific override must win.
             */
            EntryRecord specificEntry = createEntry(
                    targetLorebookId,
                    targetOutletId
            );

            assertEquals(
                    new Macro("specificOverride"),
                    outletManager.getOutletOf(specificEntry)
            );

            /*
             * Has lorebook-wide and global overrides:
             * lorebook-wide override must win.
             */
            EntryRecord lorebookEntry = createEntry(
                    targetLorebookId,
                    otherOutletId
            );

            assertEquals(
                    new Macro("lorebookOverride"),
                    outletManager.getOutletOf(lorebookEntry)
            );

            /*
             * Belongs to another lorebook:
             * only the global override applies.
             */
            EntryRecord globalEntry = createEntry(
                    otherLorebookId,
                    targetOutletId
            );

            assertEquals(
                    new Macro("globalOverride"),
                    outletManager.getOutletOf(globalEntry)
            );

            verify(outletService, never()).getOutletName(anyInt());
        }
    }

    private LorebookSnapshot mockLorebook(
            int lorebookId,
            String lorebookName
    ) {
        LorebookSnapshot snapshot = mock(LorebookSnapshot.class);

        when(snapshot.asReference())
                .thenReturn(
                        new LorebookSnapshot.Reference(lorebookId)
                );

        when(snapshot.getName())
                .thenReturn(lorebookName);

        when(lorebookManager.containsLorebook(snapshot))
                .thenReturn(true);

        return snapshot;
    }

    private EntryRecord createEntry(
            int lorebookId,
            int outletId
    ) {
        EntryRecord entry = new EntryRecord();

        entry.setLorebookId(lorebookId);
        entry.setOutlet(outletId);
        entry.setName("Test entry");

        return entry;
    }
}
