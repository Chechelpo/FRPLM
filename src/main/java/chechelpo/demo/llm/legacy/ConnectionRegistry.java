package chechelpo.demo.llm.legacy;

import chechelpo.demo.llm.backends.LLM_service;
import chechelpo.demo.utils.ID_Handler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

/** Basic connection config for LLM backend */
public final class ConnectionRegistry {
    private ConnectionRegistry() {}

    private static Logger log = LoggerFactory.getLogger(ConnectionRegistry.class.getSimpleName());
    /** ID handler. Can also be identified by name */
    private static final ID_Handler<ConnectionProfile> connections = new ID_Handler<ConnectionProfile>(ConnectionProfile.class, 50);
    private static ConnectionProfile activeProfile;

    public static ConnectionType getActiveType() {
        return activeProfile.getConnectionType();
    }
    public static WebClient getWebClient() {
        return activeProfile.getWebClient();
    }
    public static ConnectionProfile getActiveProfile() {
        return activeProfile;
    }
    public static String getURL() {
        return activeProfile.getURL();
    }
    public static LLM_service getLLmService() {
        return activeProfile.getService();
    }
    public static ConnectionProfile[] getConnectionProfiles() {
        return connections.getObjects();
    }

    public static @NotNull String setActiveConProfile(int id) {
        if (!connections.containsID(id)){
           String err = "No connection profile with ID: " + id;
           log.error(err);
           return err;
        }

        if ( activeProfile!= null ) activeProfile.setActive(false);
        activeProfile = connections.get(id);
        activeProfile.setActive(true);

        String success = "Connection profile changed to: " + activeProfile.getName();
        log.info(success);

        return success;
    }

    /**
     * Creates a new blank connection profile.
     * @param name of new profile
     * @return a msg informing of the result
     */
    public static @NotNull String createNewProfile(@NotNull String name) {
        String conflict = findMatchingName(name);
        if (conflict != null){
            conflict = "Error when creating new connection profile: " + conflict;
            log.error(conflict);
            return conflict;
        }

        int nextFreeID = connections.getNextFreeID();
        ConnectionProfile newProfile = new ConnectionProfile(nextFreeID, name,true, ConnectionType.None ,null, null);

        connections.registerObject(newProfile, nextFreeID);
        String success = "Created new connection profile: " + name;
        log.info(success);

        return success;
    }
    public static @NotNull String setProfileName(String newName) {
        return setProfileName(activeProfile.getID(), newName);
    }
    public static @NotNull String setProfileName(int id, String newName) {
        String conflict = findMatchingName(newName);
        if (conflict != null){
            conflict = "Error when changing profile name: " + conflict;
            log.warn(conflict);
            return conflict;
        }
        if (!connections.containsID(id)){
            String msg = "No connection profile with ID: " + id;
            log.error(msg);
            return msg;
        }

        connections.get(id).setName(newName);

        String success = "Updated connection profile's name: " + newName;
        log.info(success);
        return success;
    }

    public static @NotNull String setProfileURL(String newURL){
        return setProfileURL(activeProfile.getID(), newURL);
    }
    public static @NotNull String setProfileURL(int id, String newURL) {
        if (!connections.containsID(id)) {
            String conflict = "Error when setting profile URL: No connection profile with ID = " + id;
            log.error(conflict);
            return conflict;
        }

        connections.get(id).setHost(newURL);

        String msg = "Updated connection profile's URL: " + newURL;
        log.info(msg);
        return msg;
    }

    public static @NotNull String setProfileType(String newType) {
        return setProfileType(activeProfile.getID(), newType);
    }
    public static @NotNull String setProfileType(int id, String newTypeName) {
        ConnectionType newType;
        try{
            newType = ConnectionType.valueOf(newTypeName);
        } catch (IllegalArgumentException ignored) {
            String err = "No connection with type: " + newTypeName;
            log.error(err);
            return err;
        }
        if(!connections.containsID(id)){
            String err = "No connection profile with ID: " + id;
            log.error(err);
            return err;
        }

        connections.get(id).setConnectionType(newType);

        String success = "Updated connection profile's type: " + newTypeName;
        log.info(success);
        return success;
    }

    static void loadProfile(@NotNull ConnectionProfile profile) {
        connections.advancePast(profile.getID());
        connections.registerObject(profile, profile.getID());
        if (profile.isActive()) {
            activeProfile = profile;
            activeProfile.ping();
        }
    }

    public static @Nullable String findMatchingName(@NotNull String name) {
        for(ConnectionProfile profile : connections.getObjects()) //Avoid duplicate names
            if(profile != null && profile.getName().equals(name)){
                return "Duplicate: " + name;
            }
        return null;
    }
}
