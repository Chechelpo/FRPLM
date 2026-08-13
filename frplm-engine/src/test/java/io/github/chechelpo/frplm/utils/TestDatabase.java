package io.github.chechelpo.frplm.utils;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

public final class TestDatabase {

    public static DSLContext create() throws Exception {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1"
        );

        DSLContext ctx = DSL.using(connection, SQLDialect.H2);

        // Load schema.sql here

        return ctx;
    }

    private TestDatabase() {}
}