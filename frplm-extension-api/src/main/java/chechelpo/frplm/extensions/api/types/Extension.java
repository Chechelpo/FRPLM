package chechelpo.frplm.extensions.api.types;

import chechelpo.frplm.extensions.api.EngineRepository;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.Optional;
import java.util.logging.Logger;

public abstract class Extension {
    private final String extensionID;
    private final String name;
    private final String description;
    private final String source;
    private EngineRepository repository;
    private final Logger logger;
    @Contract(pure = true)
    protected Extension(String extensionID, String name , String description, String source){
        this.extensionID = extensionID;
        this.name = name;
        this.description = description;
        this.source = source;
        this.logger = Logger.getLogger("["+this.extensionID+"]");
    }
    protected Logger logger() {
        return this.logger;
    }
    public final void setRepository(EngineRepository repository) {
        if (this.repository != null) throw new IllegalStateException("The repository has already been set");
        this.repository = repository;
    }
    protected final EngineRepository getRepository() {
        return repository;
    }
    public final @NotNull String extensionId(){
        return this.extensionID;
    }
    public final @NotNull String description(){
        return this.description;
    }
    public final @NotNull String displayName(){
        return this.name;
    }
    /** Github repository of the extension for updates */
    @Contract(pure = true)
    public final @NotNull Optional<String> source(){
        return Optional.ofNullable(source);
    }

    public JsonNode defaultConfig(){
        return null;
    }
}
