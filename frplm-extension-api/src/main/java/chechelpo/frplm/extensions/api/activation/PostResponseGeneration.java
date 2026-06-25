package chechelpo.frplm.extensions.api.activation;

import chechelpo.frplm.extensions.api.session.Session;

public interface PostResponseGeneration {
    void onNewGeneration(Session session);
}
