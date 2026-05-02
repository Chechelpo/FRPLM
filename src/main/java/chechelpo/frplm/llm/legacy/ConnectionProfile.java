package chechelpo.frplm.llm.legacy;

import ch.qos.logback.classic.Logger;
import chechelpo.frplm.llm.backends.LLM_service;
import chechelpo.frplm.utils.Identifiable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

@JsonPropertyOrder("")
public final class ConnectionProfile implements Identifiable {
    private static final Logger log = (Logger) LoggerFactory.getLogger(ConnectionProfile.class);

    private final int ID;
    private String name;
    private String URL;
    private ConnectionType type;
    private String model;
    private WebClient webClient;
    private LLM_service service;
    private boolean active;
    @JsonCreator
    ConnectionProfile(
            @JsonProperty("id") int ID,
            @JsonProperty("name") String name,
            @JsonProperty("active") boolean active,
            @JsonProperty("type") ConnectionType type,
            @JsonProperty("url") String url,
            @JsonProperty("model") @Nullable String model)
    {
        this.name = name;
        this.ID = ID;
        this.active = active;
        this.type = type;
        this.URL = url;
    }
    public int getID() {
        return ID;
    }
    public String getName() {
        return name;
    }
    public ConnectionType getConnectionType() {
        return type;
    }
    public String getURL() {
        return URL;
    }
    WebClient getWebClient() {
        return webClient;
    }
    @JsonIgnore
    public LLM_service getService() {
        return service;
    }
    void setActive(boolean active) {
        this.active = active;
    }
    public boolean isActive() {
        return active;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setConnectionType(ConnectionType type) {
        this.type = type;
    }
    /**
     * @return selected model for this connection, may return null if endpoint doesn't support it.
     */
    public @Nullable String getModel() {
        return model;
    }
    /**
     * Sets new endpoint.
     * @param host prefix. Ex.: http://localhost:port
     */
    public void setHost(String host) {
        log.trace("Setting host: {}", host);
        if (type != ConnectionType.None) {
            if (updateWebClient(host)) {
                URL = host;
                service = type.create();
            }else log.error("Failed pinging host: {} on profile {}", host, this);
        } else log.error("Connection type not set");
    }

    private boolean updateWebClient(String url) {
        log.debug("Updating web client");

        WebClient newConnection = WebClient.builder()
                .baseUrl(url)
                .build();

        if (ping(newConnection)) {
            webClient = newConnection;
            return true;
        }

        return false;
    }

    /**
     * Pings LLM_backend for availability
     * @return whether a response was detected.
     */
    public boolean ping(){
        if (webClient == null) {return updateWebClient(this.URL); }
        else return ping(webClient);
    }
    private boolean ping(WebClient client) {
        if (client == null) {
            log.error("Cannot ping: WebClient is null");
            return false;
        }else if (type == ConnectionType.KOBOLD){
            try {
                client.get()
                        .uri("/api/v1/model")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                return true;
            } catch (Exception e) {
                System.err.println(e.getMessage());
                return false;
            }
        }else{
            try {
                client.get()
                        .uri("/") // or "/api" or "/v1" depending on backend
                        .retrieve()
                        .toBodilessEntity()
                        .block(); // synchronous ping

                log.trace("Ping successful: {}", URL);
                return true;
            } catch (Exception e) {
                log.warn("Ping failed for {}: {}", URL, e.getMessage());
                return false;
            }
        }
    }

    @Override
    public String toString() {
        return "Name: " + name + ", URL: " + URL + ", Type: " + type + ", Model: " + model;
    }
}
