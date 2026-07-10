package io.github.chechelpo.frplm.core.entities.pseudo_services;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;

final class TestDsl {
        static Connection newConnection() throws Exception {
            return DriverManager.getConnection(
                    "jdbc:h2:mem:test_db;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                    "sa",
                    ""
            );
        }

        static DSLContext newContext(Connection connection) {
            return DSL.using(connection, SQLDialect.H2);
        }

    static void createSchema(DSLContext ctx) {
        ctx.execute("""
        CREATE TABLE IF NOT EXISTS "PUBLIC"."TEST_TABLE"
        (
            "FIRST_ID"    INT NOT NULL,
            "SECOND_ID"   INT NOT NULL,
            "NAME"        VARCHAR(255) NOT NULL,
            "COUNTER"     INT NOT NULL DEFAULT 0,
            "DESCRIPTION" TEXT,

            CONSTRAINT "PK_TEST_TABLE"
                PRIMARY KEY ("FIRST_ID", "SECOND_ID")
        )
    """);
    }

        static void dropSchema(DSLContext ctx) {
            ctx.execute("DROP TABLE IF EXISTS \"PUBLIC\".\"TEST_TABLE\"");
        }
}