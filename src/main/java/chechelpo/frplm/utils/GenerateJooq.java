package chechelpo.frplm.utils;

import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;

public final class GenerateJooq {

    public static void main(String[] args) throws Exception {

        Configuration configuration =
                new Configuration()
                        .withJdbc(new Jdbc()
                                .withDriver("org.h2.Driver")
                                .withUrl(
                                        "jdbc:h2:mem:gen;DB_CLOSE_DELAY=-1;" +
                                                "INIT=RUNSCRIPT FROM 'classpath:db/schema.sql'"
                                )
                                .withUser("sa")
                        )
                        .withGenerator(new Generator()
                                .withDatabase(new Database()
                                        .withName("org.jooq.meta.h2.H2Database")
                                        .withInputSchema("PUBLIC")
                                        .withIncludes(".*")
                                )
                                .withTarget(new Target()
                                        .withPackageName("chechelpo.demo.jooq.generated")
                                        .withDirectory("src/main/generated")
                                )
                        );
        GenerationTool.generate(configuration);
    }
}
