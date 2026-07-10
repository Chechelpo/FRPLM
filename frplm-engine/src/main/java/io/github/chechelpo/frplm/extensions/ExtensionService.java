package io.github.chechelpo.frplm.extensions;

import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionContext;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.extensions.api.activation.PostResponseGeneration;
import io.github.chechelpo.frplm.extensions.api.activation.PrePromptGeneration;
import io.github.chechelpo.frplm.extensions.api.prompts.PromptBuilder;
import io.github.chechelpo.frplm.extensions.api.types.ConfigurableExtension;
import io.github.chechelpo.frplm.extensions.api.types.Extension;
import io.github.chechelpo.frplm.extensions.api.utils.ExtensionDBBridge;
import io.github.chechelpo.frplm.extensions.api.utils.io;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

@Service
public final class ExtensionService implements ExtensionDBBridge {
    private final Logger logger = (Logger) LoggerFactory.getLogger(ExtensionService.class.getName());

    private final List<Extension> extensions;
    private final ExtensionStore store;
    private final ExtensionRepository extensionRepository;
    private final SessionContext sessionContext;

    ExtensionService(List<Extension> extensions,
                     ExtensionStore store,
                     ServiceLoaderExtensionDiscovery extensionDiscovery,
                     ExtensionRepository extensionRepository,
                     SessionContext sessionContext
    ) {
        this.store = store;

        extensions.addAll(extensionDiscovery.discoverExtensions());

        this.extensions = extensions;
        logger.info("Extensions found: {}", extensions);
        this.extensionRepository = extensionRepository;
        this.sessionContext = sessionContext;
    }

    ExtensionRepository getExtensionRepository() {
        return extensionRepository;
    }

    @PostConstruct
    void initializeExtensions(){
        logger.info("Initializing extensions");
        extensions.stream()
                .filter(ConfigurableExtension.class::isInstance)
                .map(ConfigurableExtension.class::cast)
                .forEach(ext -> ext.setDBBridge(this));
        extensions.stream()
                .filter(ext -> !store.exists(ext.extensionId()))
                .forEach(ext -> {
                    registerExtension(ext.extensionId(), ext.defaultConfig());
                });
        extensions.forEach(ext -> ext.setRepository(extensionRepository));
    }

    private void logExtensionError(Extension e, String errorMessage){
        logger.error("Extension {} error : \n {}", e.extensionId(), errorMessage);
    }

    public void runPrePromptGeneration(SessionsRecord session, PromptBuilder builder){
        SessionImpl impl = new SessionImpl(session, extensionRepository.getContext(), sessionContext);
        extensions.stream()
                .filter(PrePromptGeneration.class::isInstance)
                .map(PrePromptGeneration.class::cast)
                .forEach(ext -> {
                    try{
                        ext.run(impl, builder);
                    }catch(RuntimeException e){
                        logExtensionError((Extension) ext, e.getMessage());
                    }
                });
    }

    public void runPostGeneration(SessionsRecord session){
        SessionImpl impl = new SessionImpl(session, extensionRepository.getContext(), sessionContext);
        extensions.stream()
                .filter(PostResponseGeneration.class::isInstance)
                .map(PostResponseGeneration.class::cast)
                .forEach(ext -> {
                        try{
                            ext.onNewGeneration(impl);
                        } catch (RuntimeException e) {
                            logExtensionError((Extension) ext, e.getMessage());
                        }
                }
        );

    }

    public void registerExtension(String extensionId, JsonNode initialConfig){
        if (!store.exists(extensionId))
            store.createExtension(extensionId, initialConfig);
    }

    public List<ConfigurableExtension> getConfigurableExtensions(){
        return extensions.stream()
                .filter(ConfigurableExtension.class::isInstance)
                .map(ConfigurableExtension.class::cast)
                .toList();
    }

    @Override
    public JsonNode getConfig(String extensionId) {
        return store.getConfig(extensionId);
    }

    @NotNull Optional<io.WebAsset> getExtensionAsset(String extensionID, String path){
        Optional<ConfigurableExtension> configExtension = extensions.stream()
                .filter(ext -> ext instanceof ConfigurableExtension && ext.extensionId().equals(extensionID))
                .map(ConfigurableExtension.class::cast)
                .findFirst();
        return configExtension.flatMap(configurableExtension -> configurableExtension.getAsset(path));
    }

    /** @apiNote the new config must include the FULL JSON, not only the changed parameters */
    @Override
    public void saveConfig(String extensionId, JsonNode newConfig) {
        store.updateConfig(extensionId, newConfig);
    }
}