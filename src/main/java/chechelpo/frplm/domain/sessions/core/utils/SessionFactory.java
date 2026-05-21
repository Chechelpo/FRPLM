package chechelpo.frplm.domain.sessions.core.utils;

import chechelpo.frplm.annotations.Factory;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityFactory;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Factory
public final class SessionFactory extends EntityFactory<SessionsRecord, Session, SessionRepository> {
    SessionFactory(SessionRepository repository) {
        super(repository);
    }

    @Contract("_ -> new")
    @Override
    protected @NotNull Session instantiate(@NotNull EntityKey<SessionsRecord> key) {
        return new Session(key, repository);
    }

}
