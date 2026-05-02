package chechelpo.frplm.config;

import chechelpo.frplm.llm.legacy.ConnectionPersister;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public final class LifeCycleManager implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        ConnectionPersister.loadAll();
    }

    @PreDestroy
    public void onShutDown() throws IOException {
        ConnectionPersister.saveAll();
    }
}
