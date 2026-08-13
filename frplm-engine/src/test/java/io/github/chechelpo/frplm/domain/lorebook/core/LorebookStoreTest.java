package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.utils.TestDatabase;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LorebookStoreTest {

    private DSLContext ctx;
    private LorebookStore store;

    @BeforeEach
    void setUp() throws Exception {
        DSLContext ctx = TestDatabase.create();

        store = new LorebookStore(ctx);

        ctx.execute("DROP ALL OBJECTS");

        ctx.execute("""
                CREATE TABLE LOREBOOKS (
                    ID INT PRIMARY KEY,
                    NAME VARCHAR(255) NOT NULL,
                    DEFAULT_OUTLET_ID INT NOT NULL,
                    NEXT_ENTRY_ID INT NOT NULL DEFAULT 0,
                    CREATED TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                )
                """);

        ctx.execute("""
                CREATE TABLE CHARACTERS (
                    ID INT PRIMARY KEY,
                    LOREBOOK_ID INT
                )
                """);

        ctx.execute("""
                CREATE TABLE LOCATIONS (
                    ID INT PRIMARY KEY,
                    LOREBOOK_ID INT
                )
                """);

        ctx.execute("""
                CREATE TABLE WORLDS (
                    ID INT PRIMARY KEY,
                    LOREBOOK_ID INT
                )
                """);
    }

    @Test
    void getGlobalLorebooksReturnsOnlyUnassociatedLorebooks() {
        ctx.insertInto(LOREBOOKS)
                .columns(
                        LOREBOOKS.ID,
                        LOREBOOKS.NAME,
                        LOREBOOKS.DEFAULT_OUTLET_ID
                )
                .values(1, "global", 1)
                .values(2, "character lorebook", 1)
                .values(3, "location lorebook", 1)
                .values(4, "world lorebook", 1)
                .execute();

        ctx.insertInto(CHARACTERS)
                .columns(CHARACTERS.ID, CHARACTERS.LOREBOOK_ID)
                .values(1, 2)
                .execute();

        ctx.insertInto(LOCATIONS)
                .columns(LOCATIONS.ID, LOCATIONS.LOREBOOK_ID)
                .values(1, 3)
                .execute();

        ctx.insertInto(WORLDS)
                .columns(WORLDS.ID, WORLDS.LOREBOOK_ID)
                .values(1, 4)
                .execute();

        List<LorebooksRecord> result = store.getGlobalLorebooks();

        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().getId());
        assertEquals("global", result.getFirst().getName());
    }
}