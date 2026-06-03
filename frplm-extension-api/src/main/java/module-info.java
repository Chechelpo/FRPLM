module chechelpo.frplm.extensions.api {
    exports chechelpo.frplm.extensions.api.types;
    exports chechelpo.frplm.extensions.api.session;
    exports chechelpo.frplm.extensions.api.results;
    exports chechelpo.frplm.extensions.api.standalone;
    exports chechelpo.frplm.extensions.api.utils;
    exports chechelpo.frplm.extensions.api;

    requires org.jetbrains.annotations;
    requires tools.jackson.databind;
    requires frplm.commons;
    requires java.logging;
}