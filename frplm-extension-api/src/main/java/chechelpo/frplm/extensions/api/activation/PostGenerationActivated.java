package chechelpo.frplm.extensions.api.activation;

import chechelpo.frplm.extensions.api.session.Session;

public interface PostGenerationActivated {
    void onNewGeneration(Session session);
}
