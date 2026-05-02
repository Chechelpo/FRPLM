package chechelpo.frplm.llm.legacy;

import org.jetbrains.annotations.Contract;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/llm")
public final class ConnectionController {
    public ConnectionController(){}
    record LLM_DTO(int id, String name, String url, String type){}
    @Contract("null -> null")
    static LLM_DTO wrapLLMConnection(ConnectionProfile profile){
        return profile == null ?
                null
                :
                new LLM_DTO(profile.getID(), profile.getName(), profile.getURL(), profile.getConnectionType().toString());
    }

    /**
     * Returns current {@link ConnectionProfile}.
     */
    @GetMapping("/get/active")
    public ResponseEntity<LLM_DTO> getActive() {
        return ResponseEntity.ok(wrapLLMConnection(ConnectionRegistry.getActiveProfile()));
    }
    @GetMapping("/get/all")
    public ResponseEntity<LLM_DTO[]> getALLProfiles() {
        ArrayList<LLM_DTO> result = new ArrayList<>();
        ConnectionProfile[] profiles = ConnectionRegistry.getConnectionProfiles();
        for (ConnectionProfile profile : profiles)
            if (profile != null)
                result.add(wrapLLMConnection(profile));

        return ResponseEntity.ok(result.toArray(new LLM_DTO[0]));
    }
    @GetMapping("/get/types")
    public ConnectionType[] getConnectionTypes() {
        return ConnectionType.values();
    }
    @PostMapping("/new")
    public String createNewProfile(@RequestParam String name){
        return ConnectionRegistry.createNewProfile(name);
    }
    @PostMapping("/set/type")
    public String setConnectionType(@RequestParam String typeName) {
        try {
            ConnectionRegistry.setProfileType(typeName);
            return "Connection type set to " + typeName;
        } catch (IllegalArgumentException e) {
            return "Unknown connection type: " + typeName;
        }
    }
    @PostMapping("/set/host")
    public String setHost(@RequestParam String host) {
        return ConnectionRegistry.setProfileURL(host);
    }
    @PostMapping("/set/active")
    public String setActive(@RequestParam int profileID) {
        return ConnectionRegistry.setActiveConProfile(profileID);
    }
}
