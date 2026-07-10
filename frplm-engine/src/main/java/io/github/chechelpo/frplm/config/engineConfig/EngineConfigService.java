package io.github.chechelpo.frplm.config.engineConfig;

import org.springframework.stereotype.Component;


public class EngineConfigService {
    private EngineConfigService() {}
    /** When a new starting location is registered, whether to add the character to existing sessions */
    private static boolean AUTO_REGISTER_NEW_CHARACTERS = true;
    /** When a new starting location is registered, whether to add the character to existing sessions */
    public boolean autoRegisterLocationsAfterSession(){
        return AUTO_REGISTER_NEW_CHARACTERS;
    }
    void setAutoRegisterNewCharacters(boolean value){
        AUTO_REGISTER_NEW_CHARACTERS = value;
    }
}
