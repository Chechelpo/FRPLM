package io.github.chechelpo.frplm.extensions;

import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionContext;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import io.github.chechelpo.frplm.extensions.snapshot_mappers.ExtensionRepository;
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
import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Service
public final class ExtensionService implements ExtensionDBBridge {
    private final Logger logger = (Logger) LoggerFactory.getLogger(ExtensionService.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
        logger.info("Extensions found: {}", extensions.stream().map(Extension::displayName).toList());
        this.extensionRepository = extensionRepository;
        this.sessionContext = sessionContext;
    }

    ExtensionRepository getExtensionRepository() {
        return extensionRepository;
    }

    @PostConstruct
    void initializeExtensions(){
        logger.info("Initializing extensions");
        extensions.forEach(ext -> ext.setExtensionDBBridge(this));
        extensions.stream()
                .filter(ext -> !store.exists(ext.extensionId()))
                .forEach(ext -> {
                    registerExtension(ext.extensionId(), ext.getDefaultConfig());
                });
        extensions.forEach(ext -> ext.setRepository(extensionRepository));
    }

    private void logExtensionError(Extension e, String errorMessage){
        logger.error("Extension {} error : \n {}", e.extensionId(), errorMessage);
    }

    void setEnabled(String extensionId, boolean value){
        this.store.setEnabled(value, extensionId);
    }

    <T extends Extension> @NonNull Optional<T> getExtensionOfType(String extensionId, Class<T> type){
        return extensions.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .filter(ext -> ext.extensionId().equals(extensionId))
                .findFirst();
    }

    public void runPrePromptGeneration(SessionsRecord session, PromptBuilder builder){
        SessionImpl impl = new SessionImpl(session, extensionRepository.getContext(), sessionContext);
        extensions.stream()
                .filter(Extension::isEnabled)
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
                .filter(Extension::isEnabled)
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

    public List<? extends ConfigurableExtension<?>> getConfigurableExtensions(){
        return extensions.stream()
                .filter(ConfigurableExtension.class::isInstance)
                .map(ext -> (ConfigurableExtension<?>) ext)
                .toList();
    }

    @Override
    public JsonNode getConfig(String extensionId) {
        return store.getConfig(extensionId)
                .orElseGet( () -> //This part is for when users inevitably make default config = null at init (I did it already), let them be forgiven for their sins
                        {
                                JsonNode config = getExtensionOfType(extensionId, ConfigurableExtension.class)
                                        .orElseThrow(() -> new EntityNotFound("No extension with id " + extensionId, Severity.SYSTEM))
                                        .getDefaultConfig();
                                saveConfig(extensionId, config); //Save new one to prevent this from happening again
                                return config;
                        }
                );
    }

    @Override
    public <T> T getConfig(String extensionId, Class<T> recordType) {
        return OBJECT_MAPPER.treeToValue(getConfig(extensionId), recordType);
    }

    @Override
    public boolean isEnabled(String extensionId) {
        return store.isEnabled(extensionId);
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