package io.github.chechelpo.frplm.domain.lorebook.outlet;

import static org.mockito.Mockito.mock;

public final class OutletServiceTestFactory {

    private OutletServiceTestFactory() {}

    public static OutletService mockService() {
        return mock(OutletServiceImpl.class);
    }
}